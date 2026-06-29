import { YearPickerField } from "@/components/YearPickerField";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { EnrollmentTerm } from "@/types";

export interface CohortBoundaryValue {
  year: number | "";
  term: EnrollmentTerm | "";
}

interface CohortBoundaryFieldsProps {
  label: string;
  description: string;
  yearId: string;
  termId: string;
  value: CohortBoundaryValue;
  onChange: (value: CohortBoundaryValue) => void;
}

const TERM_LABELS: Record<EnrollmentTerm, string> = {
  GUZ: "Güz",
  BAHAR: "Bahar",
};

/**
 * Year + term picker pair for category-course enrollment cohort boundaries.
 */
export function CohortBoundaryFields({
  label,
  description,
  yearId,
  termId,
  value,
  onChange,
}: CohortBoundaryFieldsProps) {
  return (
    <div className="space-y-2">
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <label htmlFor={yearId} className="text-xs text-muted-foreground">Yıl</label>
          <YearPickerField
            id={yearId}
            value={value.year}
            onChange={(year) => onChange({ ...value, year })}
            placeholder="Yıl seçin"
          />
        </div>
        <div className="space-y-1.5">
          <label htmlFor={termId} className="text-xs text-muted-foreground">Dönem</label>
          <Select
            value={value.term === "" ? "none" : value.term}
            onValueChange={(v) =>
              onChange({ ...value, term: v === "none" ? "" : (v as EnrollmentTerm) })
            }
          >
            <SelectTrigger id={termId} className="w-full">
              <SelectValue placeholder="Dönem seçin" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="none">—</SelectItem>
              <SelectItem value="GUZ">{TERM_LABELS.GUZ}</SelectItem>
              <SelectItem value="BAHAR">{TERM_LABELS.BAHAR}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </div>
  );
}

/** Formats a cohort boundary for display, e.g. "2017 Güz". */
export function formatCohortBoundary(year?: number | null, term?: EnrollmentTerm | null): string | null {
  if (year == null) {
    return null;
  }
  const termLabel = term ? TERM_LABELS[term] : TERM_LABELS.GUZ;
  return `${year} ${termLabel}`;
}
