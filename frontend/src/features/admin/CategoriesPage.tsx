import { CategoriesTab } from "./CategoriesTab";

/**
 * Standalone admin page for graduation category management, linked from the sidebar.
 */
export function CategoriesPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="sr-only">Mezuniyet Kategorileri</h1>
      <CategoriesTab />
    </div>
  );
}
