import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ConnectionStatus } from "@/components/dashboard/ConnectionStatus";
import { FaultList } from "@/components/dashboard/FaultList";
import { PumpCard } from "@/components/dashboard/PumpCard";
import { SingleFlightButton } from "@/components/controls/SingleFlightButton";

describe("dashboard presentation", () => {
  it("renders an unknown pump without inventing a dispensed amount", () => {
    render(<PumpCard channel="CYAN" state="UNKNOWN" dispensedGrams={null} targetGrams="2.000" fault={null} />);
    expect(screen.getByTestId("pump-state-CYAN")).toHaveTextContent("UNKNOWN");
    expect(screen.getByText("Not reported")).toBeInTheDocument();
  });

  it("renders a fault message and severity", () => {
    render(
      <FaultList
        faults={[{ severity: "ERROR", message: "Load cell reported a fault.", occurredAt: null, code: "LOAD_CELL_FAULT" }]}
      />,
    );
    expect(screen.getByText("Load cell reported a fault.")).toBeInTheDocument();
    expect(screen.getByText("ERROR")).toBeInTheDocument();
  });

  it("labels a simulated connection instead of connected", () => {
    render(
      <ConnectionStatus
        connectionState="CONNECTED"
        dataSource="SIMULATED"
        simulated
        stale={false}
        probed
      />,
    );
    expect(screen.getByTestId("connection-status")).toHaveTextContent("Simulated");
  });

  it("ignores a second click while a command is in flight", async () => {
    const user = userEvent.setup();
    let release: () => void = () => {};
    const action = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          release = resolve;
        }),
    );
    render(<SingleFlightButton label="Start" onAction={action} />);
    const button = screen.getByRole("button", { name: "Start" });
    await user.click(button);
    await user.click(button);
    expect(action).toHaveBeenCalledTimes(1);
    release();
  });
});
