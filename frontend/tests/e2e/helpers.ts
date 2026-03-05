import { expect, type Locator, type Page } from "@playwright/test";

export type TestRole = "ANALYST" | "RISK_LEAD" | "ADMIN";

const CREDENTIALS: Record<TestRole, { username: string; password: string }> = {
  ANALYST: { username: "analyst@demo.com", password: "Demo123!" },
  RISK_LEAD: { username: "risklead@demo.com", password: "Demo123!" },
  ADMIN: { username: "admin@demo.com", password: "Demo123!" },
};

export async function loginAs(page: Page, role: TestRole = "ANALYST") {
  const creds = CREDENTIALS[role];
  await page.goto("/login");
  await page.getByTestId("login-username").fill(creds.username);
  await page.getByTestId("login-password").fill(creds.password);
  const loginResponse = page.waitForResponse(
    (response) => response.url().includes("/api/v1/auth/login") && response.request().method() === "POST"
  );
  await page.getByTestId("login-submit").click();
  await expect((await loginResponse).ok()).toBeTruthy();
  await expect(page).toHaveURL(/\/app\/dashboard$/, { timeout: 15000 });
}

export async function logout(page: Page) {
  await page.getByTestId("logout-button").click();
  await expect(page).toHaveURL(/\/login$/);
}

export async function openLoans(page: Page) {
  await page.getByTestId("nav-loans").click();
  await expect(page).toHaveURL(/\/app\/loans$/);
  await expect(page.getByRole("heading", { name: "Loan Directory" })).toBeVisible();
}

export async function openFirstLoan(page: Page): Promise<number> {
  await openLoans(page);
  const firstView = page.locator('[data-testid^="loan-view-"]').first();
  await expect(firstView).toBeVisible();
  await firstView.click();
  await expect(page).toHaveURL(/\/app\/loans\/\d+\/overview$/);
  return getLoanIdFromUrl(page);
}

export function getLoanIdFromUrl(page: Page): number {
  const match = page.url().match(/\/app\/loans\/(\d+)\//);
  if (!match) {
    throw new Error(`Unable to parse loan ID from URL: ${page.url()}`);
  }
  return Number(match[1]);
}

export async function openLoanTab(
  page: Page,
  tab: "overview" | "statements" | "results" | "alerts" | "collateral" | "documents" | "comments" | "activity"
) {
  await page.getByTestId(`loan-tab-${tab}`).click();
  await expect(page).toHaveURL(new RegExp(`/app/loans/\\d+/${tab}$`));
}

export async function expectAtLeastOne(locator: Locator) {
  await expect.poll(async () => locator.count()).toBeGreaterThan(0);
}
