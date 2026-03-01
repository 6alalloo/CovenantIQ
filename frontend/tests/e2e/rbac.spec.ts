import { expect, test } from "@playwright/test";
import { loginAs } from "./helpers";

test("E2E-011 analyst navigation hides privileged areas", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await expect(page.getByTestId("nav-dashboard")).toBeVisible();
  await expect(page.getByTestId("nav-loans")).toBeVisible();
  await expect(page.getByTestId("nav-alerts")).toBeVisible();
  await expect(page.getByTestId("nav-reports")).toBeVisible();
  await expect(page.getByTestId("nav-settings")).toBeVisible();
  await expect(page.getByTestId("nav-portfolio")).toHaveCount(0);
  await expect(page.getByTestId("nav-users")).toHaveCount(0);
});

test("E2E-014 risk lead can access portfolio", async ({ page }) => {
  await loginAs(page, "RISK_LEAD");
  await expect(page.getByTestId("nav-portfolio")).toBeVisible();
  await page.getByTestId("nav-portfolio").click();
  await expect(page).toHaveURL(/\/app\/portfolio$/);
  await expect(page.getByRole("heading", { name: "Portfolio Oversight" })).toBeVisible();
});

test("E2E-016 admin can access users page", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await expect(page.getByTestId("nav-users")).toBeVisible();
  await page.getByTestId("nav-users").click();
  await expect(page).toHaveURL(/\/app\/admin\/users$/);
  await expect(page.getByRole("heading", { name: "User Management" })).toBeVisible();
});

test("E2E-017 analyst direct users route is forbidden", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await page.goto("/app/admin/users");
  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByRole("heading", { name: "Access Forbidden" })).toBeVisible();
});

test("E2E-080 analyst direct portfolio route is forbidden", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await page.goto("/app/portfolio");
  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByRole("heading", { name: "Access Forbidden" })).toBeVisible();
});

test("E2E-019 invalid app route shows not found", async ({ page }) => {
  await loginAs(page, "ANALYST");
  await page.goto("/app/not-a-real-route");
  await expect(page.getByRole("heading", { name: "Route Not Found" })).toBeVisible();
});
