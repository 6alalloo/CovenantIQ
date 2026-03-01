import { expect, test } from "@playwright/test";
import { loginAs, logout } from "./helpers";

test("E2E-001 login page loads", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByTestId("login-form")).toBeVisible();
  await expect(page.getByTestId("login-submit")).toBeVisible();
});

test("E2E-002 seeded analyst login succeeds", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await expect(page.getByRole("heading", { name: "Operational Dashboard" })).toBeVisible();
});

test("E2E-003 invalid login shows error", async ({ page }) => {
  await page.goto("/login");
  await page.getByTestId("login-username").fill("analyst@demo.com");
  await page.getByTestId("login-password").fill("wrong-password");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("login-error")).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
});

test("E2E-004 protected route redirects unauthenticated users to login", async ({ page }) => {
  await page.goto("/app/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});

test("E2E-005 session persists across refresh", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await page.reload();
  await expect(page).toHaveURL(/\/app\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Operational Dashboard" })).toBeVisible();
});

test("E2E-006 logout returns user to login screen", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await logout(page);
  await expect(page.getByTestId("login-form")).toBeVisible();
});
