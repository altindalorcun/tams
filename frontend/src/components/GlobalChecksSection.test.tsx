import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { GlobalChecksSection } from "./GlobalChecksSection";
import type { GlobalCheckResult } from "@/types";

describe("GlobalChecksSection", () => {
  it("renders nothing when no global checks are configured", () => {
    const { container } = render(<GlobalChecksSection globalCheckResults={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders passed total ECTS check", () => {
    const checks: GlobalCheckResult[] = [
      {
        checkType: "TOTAL_ECTS",
        passed: true,
        requiredMinEcts: 240,
        earnedEcts: 244,
        failedCourseCodes: [],
      },
    ];

    render(<GlobalChecksSection globalCheckResults={checks} />);

    expect(screen.getByText("Genel Bölüm Kuralları")).toBeInTheDocument();
    expect(screen.getByText(/Toplam AKTS şartı karşılandı \(244 \/ 240\)/i)).toBeInTheDocument();
    expect(screen.getByText("Yeterli")).toBeInTheDocument();
  });

  it("renders failed fail-grade check with course codes", () => {
    const checks: GlobalCheckResult[] = [
      {
        checkType: "FAIL_GRADE",
        passed: false,
        requiredMinEcts: null,
        earnedEcts: null,
        failedCourseCodes: ["BBM101", "BBM202"],
      },
    ];

    render(<GlobalChecksSection globalCheckResults={checks} />);

    expect(screen.getByText("F Notu Engeli")).toBeInTheDocument();
    expect(screen.getByText("Engellendi")).toBeInTheDocument();
    expect(screen.getByText("BBM101")).toBeInTheDocument();
    expect(screen.getByText("BBM202")).toBeInTheDocument();
  });
});
