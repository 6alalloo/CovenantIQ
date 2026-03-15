import { expect, test } from "@playwright/test";
import { loginAs, openLoans } from "./helpers";

function csvFile(name: string, body: string) {
  return {
    name,
    mimeType: "text/csv",
    buffer: Buffer.from(body, "utf-8"),
  };
}

test("E2E-090 admin can preview and execute a loan import", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.getByTestId("nav-loan-imports").click();
  await expect(page).toHaveURL(/\/app\/admin\/loan-imports$/);
  await expect(page.getByRole("heading", { name: "Loan Imports" })).toBeVisible();
  const importHistory = page.locator("div").filter({ has: page.getByRole("heading", { name: "Import History" }) });

  const fileInput = page.locator('input[type="file"]').first();
  await fileInput.setInputFiles("tests/fixtures/loan-import-valid.csv");

  await expect(page.getByText("Latest file: loan-import-valid.csv")).toBeVisible();
  await expect(page.getByRole("cell", { name: "Playwright Demo Borrower", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Run Import" })).toBeEnabled();

  const executeResponse = page.waitForResponse((response) =>
    response.url().includes("/api/v1/admin/loan-imports/") && response.url().endsWith("/execute") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Run Import" }).click();
  await expect((await executeResponse).ok()).toBeTruthy();

  await expect(page.getByText("COMPLETED", { exact: true }).first()).toBeVisible();
  await expect(importHistory.getByRole("button", { name: /Batch #/ }).first()).toBeVisible();
});

test("E2E-091 analyst cannot access loan imports route", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await page.goto("/app/admin/loan-imports");
  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByRole("heading", { name: "Access Forbidden" })).toBeVisible();
});

test("E2E-092 invalid loan import file shows validation errors", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/admin/loan-imports");

  const fileInput = page.locator('input[type="file"]').first();
  await fileInput.setInputFiles({
    name: "loan-import-invalid.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("not a csv", "utf-8"),
  });

  await expect(page.getByText("Only .csv loan import files are supported")).toBeVisible();
  await expect(page.getByRole("button", { name: "Run Import" })).toBeDisabled();
});

test("E2E-093 admin can review prior loan import history", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/admin/loan-imports");
  const importHistory = page.locator("div").filter({ has: page.getByRole("heading", { name: "Import History" }) });

  const fileInput = page.locator('input[type="file"]').first();
  const borrower = `History Borrower ${Date.now()}`;
  await fileInput.setInputFiles(
    csvFile(
      "loan-import-history.csv",
      [
        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
        `CORE_BANKING,LN-HISTORY-${Date.now()},${borrower},1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z`,
      ].join("\n")
    )
  );

  await expect(page.getByText("Latest file: loan-import-history.csv")).toBeVisible();
  await expect(page.getByText("PREVIEW_READY", { exact: true }).first()).toBeVisible();
  await expect(page.getByRole("cell", { name: borrower, exact: true })).toBeVisible();
});

test("E2E-094 unchanged loan import row is shown as UNCHANGED", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/admin/loan-imports");

  const externalId = `LN-UNCHANGED-${Date.now()}`;
  const borrower = `Unchanged Borrower ${Date.now()}`;
  const fileInput = page.locator('input[type="file"]').first();

  const firstFile = csvFile(
    "loan-import-unchanged-1.csv",
    [
      "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
      `CORE_BANKING,${externalId},${borrower},1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z`,
    ].join("\n")
  );
  await fileInput.setInputFiles(firstFile);
  await expect(page.getByRole("button", { name: "Run Import" })).toBeEnabled();
  await page.getByRole("button", { name: "Run Import" }).click();
  await expect(page.getByText("COMPLETED", { exact: true }).first()).toBeVisible();

  const secondFile = csvFile(
    "loan-import-unchanged-2.csv",
    [
      "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
      `CORE_BANKING,${externalId},${borrower},1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z`,
    ].join("\n")
  );
  await fileInput.setInputFiles(secondFile);
  await expect(page.getByRole("cell", { name: "UNCHANGED", exact: true }).first()).toBeVisible();
  await page.getByRole("button", { name: "Run Import" }).click();
  await expect(page.getByText("UNCHANGED", { exact: true }).first()).toBeVisible();
});

test("E2E-095 invalid row values are shown as preview row errors", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/admin/loan-imports");

  const fileInput = page.locator('input[type="file"]').first();
  await fileInput.setInputFiles(
    csvFile(
      "loan-import-invalid-row.csv",
      [
        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
        `CORE_BANKING,LN-INVALID-${Date.now()},Invalid Decimal,not-a-number,2025-01-15,ACTIVE,2026-03-08T10:15:00Z`,
      ].join("\n")
    )
  );

  await expect(page.getByRole("cell", { name: "ERROR", exact: true }).first()).toBeVisible();
  await expect(page.getByText("Invalid decimal value for principalAmount")).toBeVisible();
  await expect(page.getByRole("button", { name: "Run Import" })).toBeEnabled();
});

test("E2E-096 imported loan overview shows external metadata", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/admin/loan-imports");

  const externalId = `LN-META-${Date.now()}`;
  const borrower = `Metadata Borrower ${Date.now()}`;
  const fileInput = page.locator('input[type="file"]').first();
  await fileInput.setInputFiles(
    csvFile(
      "loan-import-metadata.csv",
      [
        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
        `CORE_BANKING,${externalId},${borrower},1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z`,
      ].join("\n")
    )
  );
  await page.getByRole("button", { name: "Run Import" }).click();
  await expect(page.getByText("COMPLETED", { exact: true }).first()).toBeVisible();

  await openLoans(page);
  const loanRow = page.locator("tr", { hasText: borrower }).first();
  await expect(loanRow).toBeVisible();
  await loanRow.click();

  await expect(page.getByText(`CORE_BANKING | ${externalId}`)).toBeVisible();
  await expect(page.getByText("Last Sync")).toBeVisible();
});

test("E2E-097 closed imported loan cannot be reopened through preview", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/admin/loan-imports");

  const externalId = `LN-REOPEN-${Date.now()}`;
  const borrower = `Closed Borrower ${Date.now()}`;
  const fileInput = page.locator('input[type="file"]').first();

  await fileInput.setInputFiles(
    csvFile(
      "loan-import-closed.csv",
      [
        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
        `CORE_BANKING,${externalId},${borrower},1250000.00,2025-01-15,CLOSED,2026-03-08T10:15:00Z`,
      ].join("\n")
    )
  );
  await page.getByRole("button", { name: "Run Import" }).click();
  await expect(page.getByText("COMPLETED", { exact: true }).first()).toBeVisible();

  await fileInput.setInputFiles(
    csvFile(
      "loan-import-reopen.csv",
      [
        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt",
        `CORE_BANKING,${externalId},${borrower},1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z`,
      ].join("\n")
    )
  );

  await expect(page.getByRole("cell", { name: "ERROR", exact: true }).first()).toBeVisible();
  await expect(page.getByText(`Imported loan ${externalId} cannot transition from CLOSED to ACTIVE automatically`)).toBeVisible();
});

