import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Filter } from "lucide-react";
import { ResultCard, ResultCardSkeleton, HistoryTable } from "./ResultCard";
import { getResult, getResults } from "@/api/analysisApi";
import { getDepartments } from "@/api/ruleApi";
import { DatePickerField } from "@/components/DatePickerField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  isDateFilterActive,
  matchesDateFilter,
  type DateComparisonMode,
} from "@/lib/dateFilter";
import { matchesTextFilter } from "@/lib/textFilter";

const HISTORY_FETCH_SIZE = 1000;
const HISTORY_PAGE_SIZE = 20;

const ELIGIBILITY_STATUS_ELIGIBLE = "ELIGIBLE" as const;
const ELIGIBILITY_STATUS_INELIGIBLE = "INELIGIBLE" as const;
type EligibilityStatusFilter =
  | typeof ELIGIBILITY_STATUS_ELIGIBLE
  | typeof ELIGIBILITY_STATUS_INELIGIBLE;

const ELIGIBILITY_FILTER_LABELS: Record<EligibilityStatusFilter, string> = {
  ELIGIBLE: "Uygun",
  INELIGIBLE: "Eksik",
};

const ELIGIBILITY_FILTER_ITEMS = (
  [ELIGIBILITY_STATUS_ELIGIBLE, ELIGIBILITY_STATUS_INELIGIBLE] as const
).map((status) => ({
  value: status,
  label: ELIGIBILITY_FILTER_LABELS[status],
}));

const DATE_MODE_ON = "ON" as const;
const DATE_MODE_AFTER = "AFTER" as const;
const DATE_MODE_BEFORE = "BEFORE" as const;

const DATE_MODE_FILTER_LABELS: Record<DateComparisonMode, string> = {
  ON: "Bu tarihte",
  AFTER: "Bu tarihten sonraki",
  BEFORE: "Bu tarihten önceki",
};

const DATE_MODE_FILTER_ITEMS = (
  [DATE_MODE_ON, DATE_MODE_AFTER, DATE_MODE_BEFORE] as const
).map((mode) => ({
  value: mode,
  label: DATE_MODE_FILTER_LABELS[mode],
}));

/**
 * Displays the full analysis history for all students, with client-side filters,
 * pagination, and inline detail view.
 */
export function StudentHistoryPage() {
  const [page, setPage] = useState(0);
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);
  const [studentNumberFilter, setStudentNumberFilter] = useState("");
  const [departmentFilter, setDepartmentFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<EligibilityStatusFilter | null>(null);
  const [dateFilter, setDateFilter] = useState<Date | null>(null);
  const [dateModeFilter, setDateModeFilter] = useState<DateComparisonMode | null>(null);

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ["results-history"],
    queryFn: () => getResults(0, HISTORY_FETCH_SIZE),
  });

  const { data: departments } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  const { data: selectedResult, isLoading: selectedResultLoading } = useQuery({
    queryKey: ["result", selectedResultId],
    queryFn: () => getResult(selectedResultId!),
    enabled: !!selectedResultId,
  });

  const allResults = history?.content ?? [];

  const filteredResults = useMemo(() => {
    return allResults.filter((result) => {
      if (!matchesTextFilter(result.studentNumber ?? "", studentNumberFilter)) return false;
      if (departmentFilter !== null && result.departmentId !== departmentFilter) return false;
      if (statusFilter === ELIGIBILITY_STATUS_ELIGIBLE && !result.isEligible) return false;
      if (statusFilter === ELIGIBILITY_STATUS_INELIGIBLE && result.isEligible) return false;
      if (!matchesDateFilter(result.createdAt, dateFilter, dateModeFilter)) return false;
      return true;
    });
  }, [
    allResults,
    studentNumberFilter,
    departmentFilter,
    statusFilter,
    dateFilter,
    dateModeFilter,
  ]);

  const totalFilteredPages = Math.max(1, Math.ceil(filteredResults.length / HISTORY_PAGE_SIZE));

  const pagedResults = useMemo(() => {
    const start = page * HISTORY_PAGE_SIZE;
    return filteredResults.slice(start, start + HISTORY_PAGE_SIZE);
  }, [filteredResults, page]);

  const hasActiveFilters =
    studentNumberFilter.trim() !== ""
    || departmentFilter !== null
    || statusFilter !== null
    || isDateFilterActive(dateFilter, dateModeFilter);

  const activeFilterCount = [
    studentNumberFilter.trim() !== "",
    departmentFilter !== null,
    statusFilter !== null,
    isDateFilterActive(dateFilter, dateModeFilter),
  ].filter(Boolean).length;

  useEffect(() => {
    setPage(0);
  }, [studentNumberFilter, departmentFilter, statusFilter, dateFilter, dateModeFilter]);

  useEffect(() => {
    if (page >= totalFilteredPages) {
      setPage(Math.max(0, totalFilteredPages - 1));
    }
  }, [page, totalFilteredPages]);

  function clearFilters() {
    setStudentNumberFilter("");
    setDepartmentFilter(null);
    setStatusFilter(null);
    setDateFilter(null);
    setDateModeFilter(null);
  }

  function handleDateFilterChange(date: Date | null) {
    setDateFilter(date);
    if (!date) {
      setDateModeFilter(null);
    }
  }

  const showEmptyFilterMessage =
    !historyLoading && allResults.length > 0 && filteredResults.length === 0;

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="text-2xl font-semibold">Analiz Geçmişi</h1>

      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Geçmiş Analizler</h2>
          <div className="flex items-center gap-2">
            {!historyLoading && allResults.length > 0 && (
              <Popover>
                <PopoverTrigger
                  render={
                    <Button
                      variant="outline"
                      size="sm"
                      className="transition-colors duration-150"
                      aria-pressed={hasActiveFilters}
                      aria-label="Analizleri filtrele"
                    />
                  }
                >
                  <Filter className="mr-1 h-4 w-4" />
                  Filtre
                  {hasActiveFilters && (
                    <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">
                      {activeFilterCount}
                    </Badge>
                  )}
                </PopoverTrigger>
                <PopoverContent align="end" className="w-80 shadow-md">
                  <PopoverHeader>
                    <PopoverTitle>Analizleri Filtrele</PopoverTitle>
                  </PopoverHeader>
                  <div className="flex flex-col gap-3">
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="history-student-number-filter" className="text-sm font-medium text-muted-foreground">
                        Öğrenci No
                      </label>
                      <Input
                        id="history-student-number-filter"
                        className="font-mono"
                        placeholder="Öğrenci numarasına göre filtrele"
                        value={studentNumberFilter}
                        onChange={(e) => setStudentNumberFilter(e.target.value)}
                      />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="history-department-filter" className="text-sm font-medium text-muted-foreground">
                        Bölüm
                      </label>
                      <Select
                        items={(departments ?? []).map((department) => ({
                          value: department.id,
                          label: department.name,
                        }))}
                        value={departmentFilter}
                        onValueChange={setDepartmentFilter}
                      >
                        <SelectTrigger id="history-department-filter" className="w-full">
                          <SelectValue placeholder="Bölüm seçin" />
                        </SelectTrigger>
                        <SelectContent>
                          {(departments ?? []).map((department) => (
                            <SelectItem key={department.id} value={department.id}>
                              {department.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="history-status-filter" className="text-sm font-medium text-muted-foreground">
                        Durum
                      </label>
                      <Select
                        items={ELIGIBILITY_FILTER_ITEMS}
                        value={statusFilter}
                        onValueChange={setStatusFilter}
                      >
                        <SelectTrigger id="history-status-filter" className="w-full">
                          <SelectValue placeholder="Durum seçin" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={ELIGIBILITY_STATUS_ELIGIBLE}>
                            {ELIGIBILITY_FILTER_LABELS.ELIGIBLE}
                          </SelectItem>
                          <SelectItem value={ELIGIBILITY_STATUS_INELIGIBLE}>
                            {ELIGIBILITY_FILTER_LABELS.INELIGIBLE}
                          </SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="history-date-filter" className="text-sm font-medium text-muted-foreground">
                        Tarih
                      </label>
                      <DatePickerField
                        id="history-date-filter"
                        value={dateFilter}
                        onChange={handleDateFilterChange}
                      />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="history-date-mode-filter" className="text-sm font-medium text-muted-foreground">
                        Tarih Karşılaştırma
                      </label>
                      <Select
                        items={DATE_MODE_FILTER_ITEMS}
                        value={dateModeFilter}
                        onValueChange={setDateModeFilter}
                      >
                        <SelectTrigger
                          id="history-date-mode-filter"
                          className="w-full"
                          disabled={!dateFilter}
                        >
                          <SelectValue placeholder="Karşılaştırma seçin" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={DATE_MODE_ON}>
                            {DATE_MODE_FILTER_LABELS.ON}
                          </SelectItem>
                          <SelectItem value={DATE_MODE_AFTER}>
                            {DATE_MODE_FILTER_LABELS.AFTER}
                          </SelectItem>
                          <SelectItem value={DATE_MODE_BEFORE}>
                            {DATE_MODE_FILTER_LABELS.BEFORE}
                          </SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    {hasActiveFilters && (
                      <Button variant="ghost" onClick={clearFilters} className="self-start transition-colors duration-150">
                        Temizle
                      </Button>
                    )}
                  </div>
                </PopoverContent>
              </Popover>
            )}
            {!historyLoading && filteredResults.length > 0 && totalFilteredPages > 1 && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <button
                  onClick={() => setPage((currentPage) => Math.max(0, currentPage - 1))}
                  disabled={page === 0}
                  className="disabled:opacity-40 hover:text-foreground transition-colors duration-150"
                >
                  ← Önceki
                </button>
                <span>{page + 1} / {totalFilteredPages}</span>
                <button
                  onClick={() => setPage((currentPage) => Math.min(totalFilteredPages - 1, currentPage + 1))}
                  disabled={page >= totalFilteredPages - 1}
                  className="disabled:opacity-40 hover:text-foreground transition-colors duration-150"
                >
                  Sonraki →
                </button>
              </div>
            )}
          </div>
        </div>

        {showEmptyFilterMessage && (
          <p className="text-sm text-muted-foreground text-center py-4">
            Filtreye uygun analiz bulunamadı.
          </p>
        )}

        {!showEmptyFilterMessage && (
          <HistoryTable
            results={pagedResults}
            isLoading={historyLoading}
            selectedResultId={selectedResultId}
            onSelect={(id) => setSelectedResultId(id)}
          />
        )}
      </section>

      {selectedResultId && (
        <section className="space-y-4 pt-4">
          <h2 className="text-lg font-semibold">Seçili Analiz Detayı</h2>
          {selectedResultLoading || !selectedResult ? (
            <ResultCardSkeleton />
          ) : (
            <ResultCard result={selectedResult} />
          )}
        </section>
      )}
    </div>
  );
}
