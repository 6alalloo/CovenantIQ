import { expect, test } from "@playwright/test";
import { loginAs } from "./helpers";

test("E2E-092 integrations page allows admin to create a webhook subscription", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/integrations");

  await expect(page.getByRole("heading", { name: "Integrations" })).toBeVisible();

  const stamp = Date.now();
  const subscriptionName = `E2E Webhook ${stamp}`;
  const createResponse = page.waitForResponse(
    (response) => response.url().includes("/api/v1/integrations/webhooks") && response.request().method() === "POST"
  );

  await page.getByPlaceholder("Name").fill(subscriptionName);
  await page.getByPlaceholder("Endpoint URL").fill(`https://example.test/hooks/${stamp}`);
  await page.getByPlaceholder("Secret").fill(`secret-${stamp}`);
  await page.getByPlaceholder("Comma-separated filters, e.g. AlertCreated,severity:HIGH,loanId:12").fill("AlertCreated");
  await page.getByRole("button", { name: "Create Webhook" }).click();

  await expect((await createResponse).ok()).toBeTruthy();
  const subscriptionRow = page.getByRole("button", { name: new RegExp(subscriptionName) });
  await expect(subscriptionRow).toBeVisible();
  await subscriptionRow.click();
  await expect(page.getByRole("heading", { name: "Delivery Log" })).toBeVisible();
});

test("E2E-093 workflows page loads and reflects current operating mode", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/workflows");

  await expect(page.getByRole("heading", { name: "Workflow Designer" })).toBeVisible();
  const createSampleButton = page.getByRole("button", { name: "Create Sample Alert Workflow Draft" });
  const disabledMessage = page.getByText("Sample workflow scaffolding is disabled in normal mode.");
  await expect(createSampleButton.or(disabledMessage)).toBeVisible();
});

test("E2E-094 policy studio can save and validate a draft version", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/policies");

  await expect(page.getByRole("heading", { name: "Policy Studio" })).toBeVisible();
  await expect.poll(async () => page.getByText("Covenant evaluation policies").count()).toBeGreaterThan(0);

  const summary = `E2E policy validation ${Date.now()}`;
  await page.getByLabel("Change Summary").fill(summary);
  const saveResponse = page.waitForResponse(
    (response) => /\/api\/v1\/rulesets\/\d+\/versions$/.test(response.url()) && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: /Save (Draft|Version)/ }).click();
  await expect((await saveResponse).ok()).toBeTruthy();

  const validationResponse = page.waitForResponse(
    (response) => /\/api\/v1\/rulesets\/\d+\/versions\/\d+\/validate$/.test(response.url()) && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Run Validation" }).first().click();
  await expect((await validationResponse).ok()).toBeTruthy();
  await expect(
    page.getByText("Run the sample validation to confirm the draft still downgrades active exceptions before you promote it.")
  ).toHaveCount(0);
});

test("E2E-095 change control page renders queue and timeline states", async ({ page }) => {
  await loginAs(page, "ADMIN");
  await page.goto("/app/change-control");

  await expect(page.getByRole("heading", { name: "Change Control" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Approval Queue" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Release Timeline" })).toBeVisible();

  await page.getByRole("button", { name: "Release Timeline" }).click();
  await expect(
    page.getByText("No releases yet. Approved requests will appear here once promoted.").or(
      page.getByRole("button", { name: /Rollback to this release/i }).first()
    )
  ).toBeVisible();
});
