import type { ReactNode } from "react";

export type DataColumn<T> = {
  key: string;
  header: string;
  render(row: T): ReactNode;
  className?: string;
};

export type DataTableProps<T> = {
  caption: string;
  columns: DataColumn<T>[];
  rows: T[];
  rowKey(row: T): string;
  emptyMessage?: string;
};

export function DataTable<T>({
  caption,
  columns,
  rows,
  rowKey,
  emptyMessage = "No records to show.",
}: DataTableProps<T>) {
  if (rows.length === 0) {
    return <p className="table-empty">{emptyMessage}</p>;
  }

  return (
    <div className="table-region" tabIndex={0} role="region" aria-label={caption}>
      <table className="data-table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col" className={column.className}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((column) => (
                <td key={column.key} data-label={column.header} className={column.className}>
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
