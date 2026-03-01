import { expect, test } from "@playwright/test";
import { loginAs } from "./helpers";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ANALYST");
});

test("E2E-021 dashboard loads with seeded loan selector", async ({ page }) => {
  await expect(page.getByRole("heading", { name: "Operational Dashboard" })).toBeVisible();
  const loanSelect = page.locator('select').first();
  await expect(loanSelect).toBeVisible();
  await expect.poll(async () => await loanSelect.locator("option").count()).toBeGreaterThan(0);
});

test("E2E-022 dashboard range tabs update state", async ({ page }) => {
  await page.getByRole("button", { name: "30D" }).click();
  await expect(page.getByRole("button", { name: "30D" })).toHaveAttribute("data-active", "true");
  await page.getByRole("button", { name: "All Time" }).click();
  await expect(page.getByRole("button", { name: "All Time" })).toHaveAttribute("data-active", "true");
});

test("E2E-024 quick action opens loan directory", async ({ page }) => {
  await page.getByRole("button", { name: /Open Loan Directory/i }).click();
  await expect(page).toHaveURL(/\/app\/loans$/);
  await expect(page.getByRole("heading", { name: "Loan Directory" })).toBeVisible();
});

test("E2E-025 quick action opens alert center", async ({ page }) => {
  await page.goto("/app/dashboard");
  await page.getByRole("button", { name: /Open Alert Center/i }).click();
  await expect(page).toHaveURL(/\/app\/alerts$/);
  await expect(page.getByRole("heading", { name: "Alert Center" })).toBeVisible();
});
