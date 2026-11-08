import { describe, expect, it } from "vitest";
import { formatDuration, formatTime, formatVnd } from "./format";
import { humanizeStatus } from "./status";

describe("format", () => {
  it("formats money in dong with grouping", () => {
    expect(formatVnd(85000)).toMatch(/^85[.\s ]000 ₫$/);
    expect(formatVnd(null)).toBe("-");
  });

  it("formats durations in ms below one second and seconds above", () => {
    expect(formatDuration(425)).toBe("425 ms");
    expect(formatDuration(1500)).toBe("1.5 s");
  });

  it("survives missing or invalid timestamps", () => {
    expect(formatTime(null)).toBe("-");
    expect(formatTime("not a date")).toBe("-");
    expect(formatTime("2026-10-09T04:51:50.211Z")).toMatch(/^\d{2}:\d{2}:\d{2}\.\d{3}$/);
  });
});

describe("humanizeStatus", () => {
  it("replaces every underscore", () => {
    expect(humanizeStatus("COMPENSATION_FAILED")).toBe("COMPENSATION FAILED");
  });
});
