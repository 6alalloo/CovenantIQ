import { expect, test } from "@playwright/test";
import { loginAs } from "./helpers";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ANALYST");
});

test("E2E-082 reports page loads and export history records run", async ({ page }) => {
  await page.getByTestId("nav-reports").click();
  await expect(page).toHaveURL(/\/app\/reports$/);
  await expect(page.getByRole("heading", { name: "Reports & Export" })).toBeVisible();

  const selects = page.locator("select");
  await selects.first().selectOption({ index: 1 });
  await page.getByRole("button", { name: "Export" }).click();
  await expect.poll(async () => page.locator("tbody tr").count()).toBeGreaterThan(0);
});

test("E2E-091 settings page save flow works", async ({ page }) => {
  await page.getByTestId("nav-settings").click();
  await expect(page).toHaveURL(/\/app\/settings$/);
  await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();

  await page.getByRole("switch").first().click();
  await expect(page.getByText("You have unsaved changes.")).toBeVisible();
  await page.getByRole("button", { name: "Save Changes" }).click();
  await expect(page.getByText("All changes are saved.")).toBeVisible();
});
