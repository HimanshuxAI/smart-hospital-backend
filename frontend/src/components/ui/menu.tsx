import {
  useId,
  useRef,
  useState,
  type KeyboardEvent,
  type ReactNode,
} from "react";

export type MenuItem = {
  id: string;
  label: string;
  onSelect(): void;
};

export function Menu({
  label,
  items,
  children,
}: {
  label: string;
  items: MenuItem[];
  children?: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const menuId = useId();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);

  const focusItem = (index: number) => {
    const normalized = (index + items.length) % items.length;
    itemRefs.current[normalized]?.focus();
  };

  const onMenuKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const current = itemRefs.current.findIndex((item) => item === document.activeElement);
    if (event.key === "ArrowDown") {
      event.preventDefault();
      focusItem(current + 1);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      focusItem(current - 1);
    } else if (event.key === "Home") {
      event.preventDefault();
      focusItem(0);
    } else if (event.key === "End") {
      event.preventDefault();
      focusItem(items.length - 1);
    } else if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
      buttonRef.current?.focus();
    }
  };

  return (
    <div className="menu-root">
      <button
        ref={buttonRef}
        className="button button-secondary button-sm"
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((value) => !value)}
      >
        {children ?? label}
      </button>
      {open ? (
        <div
          id={menuId}
          className="menu-list"
          role="menu"
          onKeyDown={onMenuKeyDown}
        >
          {items.map((item, index) => (
            <button
              key={item.id}
              ref={(element) => {
                itemRefs.current[index] = element;
              }}
              className="menu-item"
              role="menuitem"
              type="button"
              onClick={() => {
                item.onSelect();
                setOpen(false);
                buttonRef.current?.focus();
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}
