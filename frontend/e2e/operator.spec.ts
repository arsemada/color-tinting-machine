import { expect, test } from "@playwright/test";

test("operator sees an unverified machine and can manage a recipe", async ({ page, request }) => {
  const health = await request.get("http://127.0.0.1:8080/api/v1/health");
  expect(health.ok()).toBeTruthy();

  await page.goto("/");
  await expect(page.getByTestId("connection-status")).toBeVisible();
  await expect(page.getByText("Hardware verified")).toBeVisible();
  await expect(page.getByRole("button", { name: "Start batch" })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Request emergency stop" })).toBeDisabled();
  await expect(page.getByText(/physical emergency stop/i)).toBeVisible();

  await page.getByRole("link", { name: "Recipes" }).first().click();
  await page.getByRole("link", { name: "Create recipe" }).click();
  await page.getByLabel("Name").fill("E2E teal");
  await page.getByLabel("Cyan (g)").fill("1.25");
  await page.getByRole("button", { name: "Save recipe" }).click();
  await expect(page.getByRole("heading", { name: "Edit recipe" })).toBeVisible();

  await page.goto("/batches");
  await expect(page.getByRole("heading", { name: "Batch history" })).toBeVisible();
});
