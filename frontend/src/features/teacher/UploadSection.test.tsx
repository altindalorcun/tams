import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import { UploadSection } from "./UploadSection";
import * as ruleApi from "@/api/ruleApi";
import * as analysisApi from "@/api/analysisApi";

vi.mock("@/api/ruleApi");
vi.mock("@/api/analysisApi");

function renderUpload(onResultReady = vi.fn()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ThemeProvider attribute="class">
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <UploadSection onResultReady={onResultReady} />
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>,
  );
}

describe("UploadSection", () => {
  beforeEach(() => {
    vi.mocked(ruleApi.getDepartments).mockResolvedValue([
      { id: "dept-1", name: "Bilgisayar Mühendisliği", code: "BBM", blockOnAnyFGrade: false },
    ]);
  });

  it("renders department selector and upload area", async () => {
    renderUpload();
    await waitFor(() => {
      expect(screen.getByLabelText(/bölüm seçin/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/pdf'i buraya sürükleyin/i)).toBeInTheDocument();
  });

  it("lists departments in the selector", async () => {
    renderUpload();
    await waitFor(() => {
      expect(screen.getByText("Bilgisayar Mühendisliği")).toBeInTheDocument();
    });
  });

  it("submit button is disabled without a file and department", async () => {
    renderUpload();
    await waitFor(() => {
      expect(screen.getByLabelText(/bölüm seçin/i)).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: /analizi başlat/i })).toBeDisabled();
  });

  it("initiates upload when file and department are selected", async () => {
    const user = userEvent.setup();
    const onResultReady = vi.fn();
    vi.mocked(analysisApi.uploadTranscript).mockResolvedValueOnce({ jobId: "job-1", status: "PENDING" });
    vi.mocked(analysisApi.getJobStatus).mockResolvedValueOnce({ jobId: "job-1", status: "COMPLETED" });

    renderUpload(onResultReady);

    await waitFor(() => {
      expect(screen.getByLabelText(/bölüm seçin/i)).toBeInTheDocument();
    });

    const select = screen.getByLabelText(/bölüm seçin/i);
    await user.selectOptions(select, "dept-1");

    const file = new File(["pdf content"], "transcript.pdf", { type: "application/pdf" });
    const dropzone = screen.getByText(/pdf'i buraya sürükleyin/i).closest("div")!;
    const input = dropzone.querySelector("input[type='file']") as HTMLInputElement;
    await user.upload(input, file);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /analizi başlat/i })).not.toBeDisabled();
    });

    await user.click(screen.getByRole("button", { name: /analizi başlat/i }));

    await waitFor(() => {
      expect(analysisApi.uploadTranscript).toHaveBeenCalledWith(file, "dept-1");
    });
  });
});
