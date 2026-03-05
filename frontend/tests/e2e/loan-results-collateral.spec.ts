import { expect, test } from "@playwright/test";
import { loginAs, openLoanTab, openLoans } from "./helpers";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ADMIN");
  await openLoans(page);

  const borrower = `E2E Collateral ${Date.now()}`;
  await page.getByPlaceholder("Borrower name").fill(borrower);
  await page.getByPlaceholder("Principal amount").fill("1500000");
  await page.locator('input[type="date"]').fill("2026-02-01");
  await page.getByRole("button", { name: "Create" }).click();

  const createdRow = page.locator("tr", { hasText: borrower }).first();
  await expect(createdRow).toBeVisible();
  await createdRow.getByRole("link", { name: "View" }).click();
  await expect(page).toHaveURL(/\/app\/loans\/\d+\/overview$/);

  await page.getByPlaceholder("Threshold value").fill("1.2");
  const covenantResponse = page.waitForResponse(
    (response) => response.url().includes("/covenants") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Save Covenant" }).click();
  await expect((await covenantResponse).ok()).toBeTruthy();

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
  const statementResponse = page.waitForResponse(
    (response) => response.url().includes("/financial-statements") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Submit" }).click();
  await expect((await statementResponse).ok()).toBeTruthy();
});

test("E2E-096 results tab shows evaluation rows and filter works", async ({ page }) => {
  await openLoanTab(page, "results");

  await expect(page.getByRole("heading", { name: "Covenant Evaluation Timeline" })).toBeVisible();
  await expect.poll(async () => page.locator("tbody tr").count()).toBeGreaterThan(0);

  await page.locator("select").first().selectOption("PASS");
  await expect.poll(async () => page.locator("tbody tr").count()).toBeGreaterThan(0);
  await expect(page.locator("tbody tr").first()).toContainText("Pass");
});

test("E2E-097 collateral tab adds collateral and requests an exception", async ({ page }) => {
  await openLoanTab(page, "collateral");

  const collateralResponse = page.waitForResponse(
    (response) => response.url().includes("/collaterals") && response.request().method() === "POST"
  );
  await page.getByPlaceholder("Asset type").fill("INVENTORY");
  await page.getByPlaceholder("Nominal value").fill("500000");
  await page.getByPlaceholder("Haircut pct (0-1)").fill("0.15");
  await page.getByPlaceholder("Lien rank").fill("1");
  await page.getByPlaceholder("Currency").fill("USD");
  await page.getByRole("button", { name: "Add Collateral" }).click();
  await expect((await collateralResponse).ok()).toBeTruthy();
  await expect(page.getByText(/INVENTORY \| nominal 500000/)).toBeVisible();

  const initialExceptionCards = await page.getByRole("button", { name: "Expire" }).count();
  const exceptionResponse = page.waitForResponse(
    (response) => response.url().includes("/exceptions") && response.request().method() === "POST"
  );
  await page.getByPlaceholder("Reason").fill(`E2E waiver ${Date.now()}`);
  await page.getByRole("button", { name: "Request Exception" }).click();
  await expect((await exceptionResponse).ok()).toBeTruthy();
  await expect.poll(async () => page.getByRole("button", { name: "Expire" }).count()).toBeGreaterThan(initialExceptionCards);
});
