import { useId, useRef, useState, type KeyboardEvent, type ReactNode } from "react";

export type TabItem = {
  id: string;
  label: string;
  content?: ReactNode;
};

export function Tabs({
  tabs,
  ariaLabel,
  defaultTabId,
}: {
  tabs: TabItem[];
  ariaLabel: string;
  defaultTabId?: string;
}) {
  const baseId = useId();
  const [selectedId, setSelectedId] = useState(defaultTabId ?? tabs[0]?.id ?? "");
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);

  const select = (id: string) => {
    setSelectedId(id);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    let nextIndex: number | undefined;
    if (event.key === "ArrowRight") {
      nextIndex = (index + 1) % tabs.length;
    } else if (event.key === "ArrowLeft") {
      nextIndex = (index - 1 + tabs.length) % tabs.length;
    } else if (event.key === "Home") {
      nextIndex = 0;
    } else if (event.key === "End") {
      nextIndex = tabs.length - 1;
    }

    if (nextIndex === undefined) {
      return;
    }
    event.preventDefault();
    const next = tabs[nextIndex];
    if (next) {
      select(next.id);
      tabRefs.current[nextIndex]?.focus();
    }
  };

  const selected = tabs.find((tab) => tab.id === selectedId);

  return (
    <div className="tabs">
      <div className="tab-list" role="tablist" aria-label={ariaLabel}>
        {tabs.map((tab, index) => (
          <button
            key={tab.id}
            ref={(element) => {
              tabRefs.current[index] = element;
            }}
            id={`${baseId}-tab-${tab.id}`}
            className="tab"
            type="button"
            role="tab"
            aria-selected={tab.id === selectedId}
            aria-controls={`${baseId}-panel-${tab.id}`}
            tabIndex={tab.id === selectedId ? 0 : -1}
            onClick={() => select(tab.id)}
            onKeyDown={(event) => onKeyDown(event, index)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {selected ? (
        <div
          id={`${baseId}-panel-${selected.id}`}
          className="tab-panel"
          role="tabpanel"
          aria-labelledby={`${baseId}-tab-${selected.id}`}
        >
          {selected.content}
        </div>
      ) : null}
    </div>
  );
}
