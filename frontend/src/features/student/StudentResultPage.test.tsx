import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { StudentResultPage } from "./StudentResultPage";
import * as analysisApi from "@/api/analysisApi";
import type { AnalysisResult } from "@/types";

vi.mock("@/api/analysisApi");

const MOCK_RESULT: AnalysisResult = {
  id: "result-1",
  jobId: "job-1",
  studentNumber: "21627208",
  departmentId: "dept-1",
  departmentName: "Bilgisayar Mühendisliği",
  status: "COMPLETED",
  isEligible: true,
  gpa: 3.42,
  totalCredit: 120,
  totalEcts: 180,
  createdAt: "2026-01-15T10:00:00Z",
  categoryResults: [
    {
      categoryId: "cat-1",
      categoryName: "Zorunlu Dersler",
      satisfied: true,
      earnedCourseCount: 10,
      requiredCourseCount: 10,
      earnedCredit: 60,
      requiredCredit: 60,
      earnedEcts: 90,
      requiredEcts: 90,
      missingMandatoryCourses: [],
    },
  ],
};

const INELIGIBLE_RESULT: AnalysisResult = {
  ...MOCK_RESULT,
  isEligible: false,
  categoryResults: [
    {
      ...MOCK_RESULT.categoryResults[0],
      satisfied: false,
      earnedCourseCount: 8,
      missingMandatoryCourses: ["BBM301"],
    },
  ],
};

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ThemeProvider attribute="class">
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <StudentResultPage />
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>,
  );
}

describe("StudentResultPage", () => {
  it("shows eligible status when student has met all requirements", async () => {
    vi.mocked(analysisApi.getMyResult).mockResolvedValueOnce(MOCK_RESULT);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/mezuniyete hak kazandınız/i)).toBeInTheDocument();
    });
    expect(screen.getByText("3.42")).toBeInTheDocument();
  });

  it("shows ineligible status and deficiency list", async () => {
    vi.mocked(analysisApi.getMyResult).mockResolvedValueOnce(INELIGIBLE_RESULT);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/mezuniyete hak kazanamadınız/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/BBM301/i)).toBeInTheDocument();
  });

  it("shows empty state when no result exists", async () => {
    vi.mocked(analysisApi.getMyResult).mockRejectedValueOnce(new Error("Not Found"));
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/sonuç bulunamadı/i)).toBeInTheDocument();
    });
  });

  it("displays category progress", async () => {
    vi.mocked(analysisApi.getMyResult).mockResolvedValueOnce(MOCK_RESULT);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("Zorunlu Dersler")).toBeInTheDocument();
    });
  });
});
