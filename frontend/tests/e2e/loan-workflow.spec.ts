import { expect, test } from "@playwright/test";
import { loginAs, openLoanTab, openLoans } from "./helpers";

const FIXTURE_VALID_CSV = "tests/fixtures/valid-bulk-import.csv";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ANALYST");
  await openLoans(page);

  const borrower = `E2E Workflow ${Date.now()}`;
  await page.getByPlaceholder("Borrower name").fill(borrower);
  await page.getByPlaceholder("Principal amount").fill("1000000");
  await page.locator('input[type="date"]').fill("2026-02-01");
  await page.getByRole("button", { name: "Create" }).click();
  const createdRow = page.locator("tr", { hasText: borrower }).first();
  await expect(createdRow).toBeVisible();
  await createdRow.getByRole("link", { name: "View" }).click();
  await expect(page).toHaveURL(/\/app\/loans\/\d+\/overview$/);

  await page.getByPlaceholder("Threshold value").fill("1.2");
  const initialCovenantResponse = page.waitForResponse((response) =>
    response.url().includes("/covenants") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Save Covenant" }).click();
  await expect((await initialCovenantResponse).ok()).toBeTruthy();
});

test("E2E-038 loan detail tabs are accessible", async ({ page }) => {
  await openLoanTab(page, "overview");
  await openLoanTab(page, "statements");
  await openLoanTab(page, "results");
  await openLoanTab(page, "alerts");
  await openLoanTab(page, "documents");
  await openLoanTab(page, "comments");
  await openLoanTab(page, "activity");
});

test("E2E-041 overview loads borrower snapshot and covenant list", async ({ page }) => {
  await openLoanTab(page, "overview");
  await expect(page.getByRole("heading", { name: "Borrower Snapshot" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Add Covenant" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Covenant List" })).toBeVisible();
});

test("E2E-042 add covenant succeeds", async ({ page }) => {
  await openLoanTab(page, "overview");
  await page.locator("select").first().selectOption("DEBT_TO_EQUITY");
  await page.getByPlaceholder("Threshold value").fill("1.11");
  const createResponse = page.waitForResponse((response) =>
    response.url().includes("/api/v1/loans/") &&
    response.url().includes("/covenants") &&
    response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Save Covenant" }).click();
  await expect((await createResponse).ok()).toBeTruthy();
  await expect(page.getByPlaceholder("Threshold value")).toHaveValue("");
});

test("E2E-060 statements tab loads history and submits statement", async ({ page }) => {
  await openLoanTab(page, "statements");
  await expect(page.getByRole("heading", { name: "Statement History" })).toBeVisible();
  const year = String(new Date().getFullYear() + 5);
  await page.getByPlaceholder("Fiscal year").fill(year);
  await page.getByRole("combobox").nth(1).selectOption("4");
  await page.getByPlaceholder("Current assets").fill("250");
  await page.getByPlaceholder("Current liabilities").fill("125");
  await page.getByPlaceholder("Total debt").fill("300");
  await page.getByPlaceholder("Total equity").fill("200");
  await page.getByPlaceholder("EBIT").fill("80");
  await page.getByPlaceholder("Interest expense").fill("20");
  const submitResponse = page.waitForResponse((response) =>
    response.url().includes("/api/v1/loans/") &&
    response.url().includes("/financial-statements") &&
    response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Submit" }).click();
  await expect((await submitResponse).ok()).toBeTruthy();
  await expect(page.getByText(/submitted successfully/i)).toBeVisible();
});

test("E2E-064 statements bulk import works", async ({ page }) => {
  await openLoanTab(page, "statements");
  await page.locator('input[type="file"]').setInputFiles(FIXTURE_VALID_CSV);
  const importResponse = page.waitForResponse((response) =>
    response.url().includes("/bulk-import") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Run Bulk Import" }).click();
  await expect((await importResponse).ok()).toBeTruthy();
  await expect(page.getByText(/Bulk import complete:/)).toBeVisible();
});

test("E2E-066 documents upload and delete works", async ({ page }) => {
  await submitStatement(page);
  await openLoanTab(page, "documents");
  await expect(page.getByRole("heading", { name: "Statement Attachments" })).toBeVisible();
  const filename = `sample-upload-${Date.now()}.pdf`;
  const initialRows = await page.locator("tbody tr").count();
  await page.locator('input[type="file"]').setInputFiles({
    name: filename,
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n"),
  });
  const uploadResponse = page.waitForResponse((response) =>
    response.url().includes("/attachments") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Upload" }).click();
  await expect((await uploadResponse).ok()).toBeTruthy();
  const row = page.locator("tr", { hasText: filename }).first();
  await expect(row).toBeVisible();

  const deleteResponse = page.waitForResponse((response) =>
    response.url().includes("/attachments/") && response.request().method() === "DELETE"
  );
  await row.getByRole("button", { name: "Delete" }).click();
  const deleted = await deleteResponse;
  if (deleted.ok()) {
    await expect(page.locator("tr", { hasText: filename })).toHaveCount(0);
    await expect.poll(async () => page.locator("tbody tr").count()).toBe(initialRows);
  } else {
    await expect(deleted.status()).toBe(403);
    await expect(page.getByText("HTTP 403")).toBeVisible();
  }
});

test("E2E-072 comments add and delete works", async ({ page }) => {
  await openLoanTab(page, "comments");
  const note = `E2E comment ${Date.now()}`;
  const initialCount = await page.locator("article").count();
  await page.getByPlaceholder("Add a comment with context or handoff notes").fill(note);
  const addResponse = page.waitForResponse((response) =>
    response.url().includes("/comments") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Post" }).click();
  await expect((await addResponse).ok()).toBeTruthy();

  const article = page.locator("article", { hasText: note });
  await expect(article).toBeVisible();
  const deleteResponse = page.waitForResponse((response) =>
    response.url().includes("/comments/") && response.request().method() === "DELETE"
  );
  await article.getByRole("button", { name: "Delete" }).click();
  const deleted = await deleteResponse;
  if (deleted.ok()) {
    await expect(page.locator("article", { hasText: note })).toHaveCount(0);
    await expect.poll(async () => page.locator("article").count()).toBe(initialCount);
  } else {
    await expect(deleted.status()).toBe(403);
    await expect(page.getByText("HTTP 403")).toBeVisible();
  }
});

test("E2E-075 activity tab loads rows", async ({ page }) => {
  await openLoanTab(page, "activity");
  await expect(page.getByRole("heading", { name: "Loan Activity Trail" })).toBeVisible();
  await expect.poll(async () => page.locator("tbody tr").count()).toBeGreaterThan(0);
});

async function submitStatement(page: import("@playwright/test").Page) {
  await openLoanTab(page, "statements");
  const year = String(new Date().getFullYear() + 5);
  await page.getByPlaceholder("Fiscal year").fill(year);
  await page.getByRole("combobox").nth(1).selectOption("4");
  await page.getByPlaceholder("Current assets").fill("250");
  await page.getByPlaceholder("Current liabilities").fill("125");
  await page.getByPlaceholder("Total debt").fill("300");
  await page.getByPlaceholder("Total equity").fill("200");
  await page.getByPlaceholder("EBIT").fill("80");
  await page.getByPlaceholder("Interest expense").fill("20");
  const submitResponse = page.waitForResponse((response) =>
    response.url().includes("/financial-statements") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Submit" }).click();
  await expect((await submitResponse).ok()).toBeTruthy();
}
