import { expect, test } from "@playwright/test";
import { loginAs, openFirstLoan, openLoanTab } from "./helpers";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ANALYST");
});

test("E2E-051 global alerts page loads", async ({ page }) => {
  await page.getByTestId("nav-alerts").click();
  await expect(page).toHaveURL(/\/app\/alerts$/);
  await expect(page.getByRole("heading", { name: "Alert Center" })).toBeVisible();
  await expect.poll(async () => page.locator("tbody tr").count()).toBeGreaterThan(0);
});

test("E2E-053 global alerts OPEN filter works", async ({ page }) => {
  await page.goto("/app/alerts");
  await page.locator("select").first().selectOption("OPEN");
  const rows = page.locator("tbody tr");
  const count = await rows.count();
  for (let i = 0; i < count; i++) {
    await expect(rows.nth(i)).toContainText("Open");
  }
});

test("E2E-059 view loan link opens loan alerts with focusAlert", async ({ page }) => {
  await page.goto("/app/alerts");
  const firstView = page.getByRole("link", { name: "View Loan" }).first();
  await expect(firstView).toBeVisible();
  await firstView.click();
  await expect(page).toHaveURL(/\/app\/loans\/\d+\/alerts\?focusAlert=\d+$/);
  await expect(page.getByRole("heading", { name: "Loan Alert Operations" })).toBeVisible();
});

test("E2E-048 loan alert can be resolved", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await openFirstLoan(page);
  await openLoanTab(page, "alerts");
  const resolveButton = page.locator('[data-testid^="alert-resolve-"]').first();
  await expect(resolveButton).toBeVisible();
  const testId = await resolveButton.getAttribute("data-testid");
  if (!testId) {
    throw new Error("Unable to resolve alert test id.");
  }
  const alertId = testId.replace("alert-resolve-", "");
  const statusCell = page.getByTestId(`alert-status-${alertId}`);
  const currentStatus = (await statusCell.innerText()).trim().toLowerCase();

  if (currentStatus.includes("open")) {
    const ackButton = page.getByTestId(`alert-ack-${alertId}`);
    await expect(ackButton).toBeVisible();
    await ackButton.click();
    await expect(statusCell).toContainText("Acknowledged");
  }

  await page.getByTestId(`alert-resolve-${alertId}`).click();
  await expect(page.getByTestId(`alert-status-${alertId}`)).toContainText("Resolved");
});
