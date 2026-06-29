import { YearPickerField } from "@/components/YearPickerField";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  ENROLLMENT_TERM_LABELS,
  ENROLLMENT_TERM_NONE,
  ENROLLMENT_TERM_SELECT_ITEMS,
  ENROLLMENT_TERM_UNSET_LABEL,
} from "@/lib/enrollmentTermSelect";
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
            items={ENROLLMENT_TERM_SELECT_ITEMS}
            value={value.term === "" ? ENROLLMENT_TERM_NONE : value.term}
            onValueChange={(v) =>
              onChange({ ...value, term: v === ENROLLMENT_TERM_NONE ? "" : (v as EnrollmentTerm) })
            }
          >
            <SelectTrigger id={termId} className="w-full [&_[data-slot=select-value]]:line-clamp-none">
              <SelectValue placeholder="Dönem seçin" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ENROLLMENT_TERM_NONE}>{ENROLLMENT_TERM_UNSET_LABEL}</SelectItem>
              <SelectItem value="GUZ">{ENROLLMENT_TERM_LABELS.GUZ}</SelectItem>
              <SelectItem value="BAHAR">{ENROLLMENT_TERM_LABELS.BAHAR}</SelectItem>
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
  const termLabel = term ? ENROLLMENT_TERM_LABELS[term] : ENROLLMENT_TERM_LABELS.GUZ;
  return `${year} ${termLabel}`;
}
