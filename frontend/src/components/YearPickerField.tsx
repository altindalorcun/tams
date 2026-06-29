import { useEffect, useMemo, useState } from "react";
import { CalendarIcon, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import {
  COHORT_YEAR_MAX,
  COHORT_YEAR_MIN,
  clampCohortYear,
  getDecadeStart,
  getYearsInDecade,
  parseCohortYearInput,
} from "@/lib/cohortYears";

interface YearPickerFieldProps {
  id?: string;
  value: number | "" | undefined;
  onChange: (value: number | "") => void;
  placeholder?: string;
  minYear?: number;
  maxYear?: number;
  className?: string;
}

function resolveViewDecade(
  selectedYear: number | null,
  minYear: number,
  maxYear: number,
): number {
  const minDecade = getDecadeStart(minYear);
  const maxDecade = getDecadeStart(maxYear);
  const anchor = selectedYear ?? new Date().getFullYear();
  const decade = getDecadeStart(clampCohortYear(anchor, minYear, maxYear));
  return Math.min(maxDecade, Math.max(minDecade, decade));
}

/**
 * Hybrid enrollment-year picker: direct numeric input plus a decade-navigated popover.
 * Used for optional cohort range fields where empty means no bound.
 */
export function YearPickerField({
  id,
  value,
  onChange,
  placeholder = "Yıl seçin",
  minYear = COHORT_YEAR_MIN,
  maxYear = COHORT_YEAR_MAX,
  className,
}: YearPickerFieldProps) {
  const [open, setOpen] = useState(false);
  const selectedYear = value === "" || value === undefined ? null : Number(value);
  const [inputText, setInputText] = useState(selectedYear !== null ? String(selectedYear) : "");
  const [viewDecade, setViewDecade] = useState(() =>
    resolveViewDecade(selectedYear, minYear, maxYear),
  );

  const minDecade = getDecadeStart(minYear);
  const maxDecade = getDecadeStart(maxYear);

  const visibleYears = useMemo(
    () => getYearsInDecade(viewDecade, minYear, maxYear),
    [viewDecade, minYear, maxYear],
  );

  const decadeLabel =
    visibleYears.length > 0
      ? `${visibleYears[0]}–${visibleYears[visibleYears.length - 1]}`
      : `${viewDecade}–${viewDecade + 9}`;

  useEffect(() => {
    setInputText(selectedYear !== null ? String(selectedYear) : "");
  }, [selectedYear]);

  useEffect(() => {
    if (open) {
      setViewDecade(resolveViewDecade(selectedYear, minYear, maxYear));
    }
  }, [open, selectedYear, minYear, maxYear]);

  function handleInputChange(event: React.ChangeEvent<HTMLInputElement>) {
    const next = event.target.value;
    setInputText(next);
    if (next.trim() === "") {
      onChange("");
    }
  }

  function handleInputBlur() {
    const parsed = parseCohortYearInput(inputText, minYear, maxYear);
    if (parsed === "") {
      setInputText("");
      onChange("");
      return;
    }
    const clamped = clampCohortYear(parsed, minYear, maxYear);
    setInputText(String(clamped));
    onChange(clamped);
  }

  function handleSelect(year: number) {
    onChange(year);
    setInputText(String(year));
    setOpen(false);
  }

  function handleClear() {
    onChange("");
    setInputText("");
    setOpen(false);
  }

  return (
    <div className={cn("flex gap-1.5", className)}>
      <Input
        id={id}
        type="number"
        min={minYear}
        max={maxYear}
        placeholder={placeholder}
        value={inputText}
        onChange={handleInputChange}
        onBlur={handleInputBlur}
        className="flex-1 tabular-nums"
      />
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          render={
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="shrink-0 transition-colors duration-150"
              aria-label="Yıl seç"
            />
          }
        >
          <CalendarIcon className="h-4 w-4" />
        </PopoverTrigger>
        <PopoverContent align="end" className="w-64 p-3 shadow-md">
          <div className="flex items-center justify-between gap-2 mb-2">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-7 w-7 shrink-0 transition-colors duration-150"
              aria-label="Önceki on yıl"
              disabled={viewDecade <= minDecade}
              onClick={() => setViewDecade((d) => Math.max(minDecade, d - 10))}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <p className="text-xs font-medium text-muted-foreground text-center">{decadeLabel}</p>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-7 w-7 shrink-0 transition-colors duration-150"
              aria-label="Sonraki on yıl"
              disabled={viewDecade >= maxDecade}
              onClick={() => setViewDecade((d) => Math.min(maxDecade, d + 10))}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
          <div className="grid grid-cols-4 gap-1">
            {visibleYears.map((year) => {
              const isSelected = selectedYear === year;
              return (
                <button
                  key={year}
                  type="button"
                  className={cn(
                    "rounded-md px-1 py-1.5 text-sm tabular-nums transition-colors duration-150 hover:bg-muted/50",
                    isSelected && "bg-muted font-medium",
                  )}
                  aria-pressed={isSelected}
                  onClick={() => handleSelect(year)}
                >
                  {year}
                </button>
              );
            })}
          </div>
          {selectedYear !== null && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="mt-2 w-full transition-colors duration-150"
              onClick={handleClear}
            >
              Temizle
            </Button>
          )}
        </PopoverContent>
      </Popover>
    </div>
  );
}
