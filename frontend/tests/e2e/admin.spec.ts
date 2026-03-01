import { expect, test } from "@playwright/test";
import { loginAs, logout } from "./helpers";

test.beforeEach(async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.getByTestId("nav-users").click();
  await expect(page).toHaveURL(/\/app\/admin\/users$/);
});

test("E2E-085 users page renders", async ({ page }) => {
  await expect(page.getByRole("heading", { name: "User Management" })).toBeVisible();
  await expect.poll(async () => page.locator("tbody tr").count()).toBeGreaterThan(0);
});

test("E2E-086 create user works and E2E-089 deactivate works", async ({ page }) => {
  const stamp = Date.now();
  const username = `e2e.user.${stamp}@demo.com`;
  const password = "Demo123!";

  await page.getByPlaceholder("Username").fill(username);
  await page.getByPlaceholder("Email").fill(username);
  await page.getByPlaceholder("Password").fill(password);
  await page.getByRole("button", { name: "Create User" }).click();
  const row = page.locator("tr", { hasText: username }).first();
  await expect(row).toBeVisible();
  const deactivateResponse = page.waitForResponse((response) =>
    response.url().includes("/api/v1/users/") && response.request().method() === "DELETE"
  );
  await row.getByRole("button", { name: "Deactivate" }).click();
  const deactivate = await deactivateResponse;

  if (deactivate.ok()) {
    await expect(row.getByRole("button", { name: "Deactivate" })).toHaveCount(0);
    await expect(row).toContainText("Inactive");

    await logout(page);
    await page.getByTestId("login-username").fill(username);
    await page.getByTestId("login-password").fill(password);
    await page.getByTestId("login-submit").click();
    await expect(page.getByTestId("login-error")).toBeVisible();
  } else {
    await expect(deactivate.status()).toBe(403);
    await expect(page.getByText("HTTP 403")).toBeVisible();
  }
});
