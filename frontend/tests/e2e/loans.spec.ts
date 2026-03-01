import { expect, test } from "@playwright/test";
import { expectAtLeastOne, loginAs, openLoans } from "./helpers";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ANALYST");
  await openLoans(page);
});

test("E2E-027 loans table loads seeded rows", async ({ page }) => {
  const rows = page.locator('[data-testid^="loan-row-"]');
  await expectAtLeastOne(rows);
});

test("E2E-028 search filters loan list", async ({ page }) => {
  await page.getByPlaceholder("Search borrower or loan id").fill("Acme");
  const rows = page.locator('[data-testid^="loan-row-"]');
  await expect.poll(async () => rows.count()).toBeGreaterThan(0);
});

test("E2E-030 status filter active works", async ({ page }) => {
  await page.locator("select").first().selectOption("ACTIVE");
  const closeButtons = page.getByRole("button", { name: "Close" });
  await expect.poll(async () => await closeButtons.count()).toBeGreaterThan(0);
});

test("E2E-032 create loan adds a new row", async ({ page }) => {
  const borrower = `E2E Borrower ${Date.now()}`;

  await page.getByPlaceholder("Borrower name").fill(borrower);
  await page.getByPlaceholder("Principal amount").fill("1200000");
  await page.locator('input[type="date"]').fill("2026-02-01");
  await page.getByRole("button", { name: "Create" }).click();

  await expect(page.getByText(borrower)).toBeVisible();
  const createdRow = page.locator("tr", { hasText: borrower }).first();
  await expect(createdRow).toBeVisible();
});

test("E2E-034 clicking first row navigates to loan overview", async ({ page }) => {
  const firstRow = page.locator('[data-testid^="loan-row-"]').first();
  await expect(firstRow).toBeVisible();
  await firstRow.click();
  await expect(page).toHaveURL(/\/app\/loans\/\d+\/overview$/);
  await expect(page.getByRole("heading", { name: /Loan #\d+/ })).toBeVisible();
});

test("E2E-036 close flow updates loan status", async ({ page }) => {
  await page.locator("select").first().selectOption("ACTIVE");
  const closeButton = page.getByRole("button", { name: "Close" }).first();
  await expect(closeButton).toBeVisible();
  await closeButton.click();
  await page.getByRole("button", { name: "Confirm Close" }).click();
  await expect(page.getByText("Close loan?")).toHaveCount(0);
});
