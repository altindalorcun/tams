import { useMemo, useState } from "react";
import { CalendarIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import {
  COHORT_YEAR_MAX,
  COHORT_YEAR_MIN,
  groupYearsByDecade,
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

/**
 * Enrollment-year picker with decade-grouped clickable year cells.
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
  const decadeGroups = useMemo(
    () => groupYearsByDecade(minYear, maxYear),
    [minYear, maxYear],
  );

  const selectedYear = value === "" || value === undefined ? null : Number(value);

  function handleSelect(year: number) {
    onChange(year);
    setOpen(false);
  }

  function handleClear() {
    onChange("");
    setOpen(false);
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        id={id}
        render={
          <Button
            variant="outline"
            className={cn(
              "w-full justify-start font-normal transition-colors duration-150",
              selectedYear === null && "text-muted-foreground",
              className,
            )}
          />
        }
      >
        <CalendarIcon className="mr-2 h-4 w-4" />
        {selectedYear !== null ? selectedYear : placeholder}
      </PopoverTrigger>
      <PopoverContent align="start" className="w-64 p-3 shadow-md">
        <div className="max-h-64 overflow-y-auto space-y-3">
          {decadeGroups.map((group) => (
            <div key={group.label}>
              <p className="text-xs font-medium text-muted-foreground mb-1.5">{group.label}</p>
              <div className="grid grid-cols-4 gap-1">
                {group.years.map((year) => {
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
            </div>
          ))}
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
  );
}
