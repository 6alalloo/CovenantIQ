import { expect, test } from "@playwright/test";
import { loginAs } from "./helpers";

test("E2E-090 admin can preview and execute a loan import", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.getByTestId("nav-loan-imports").click();
  await expect(page).toHaveURL(/\/app\/admin\/loan-imports$/);
  await expect(page.getByRole("heading", { name: "Loan Imports" })).toBeVisible();

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
  await expect(page.getByText(/Batch #/)).toBeVisible();
});

test("E2E-091 analyst cannot access loan imports route", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await page.goto("/app/admin/loan-imports");
  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByRole("heading", { name: "Access Forbidden" })).toBeVisible();
});

