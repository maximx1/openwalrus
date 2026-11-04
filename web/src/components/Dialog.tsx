import { useEffect, useId, useRef, type ReactNode } from "react";
import { CloseIcon } from "./Icons.tsx";

/** A modal box with a title and ×. Closes with ×, Escape, or a click outside it. */
type Props = { title: string; onClose: () => void; wide?: boolean; children: ReactNode };

export function Dialog({ title, onClose, wide = false, children }: Props) {
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    dialog.current?.showModal();
  }, []);

  return (
    <dialog
      ref={dialog}
      className={`dialog${wide ? " wide" : ""}`}
      aria-labelledby={titleId}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === dialog.current) dialog.current.close(); // the backdrop, outside the box
      }}
    >
      <div className="dialog-box">
        <header>
          <h2 id={titleId}>{title}</h2>
          <button type="button" className="close" aria-label="Close" onClick={() => dialog.current?.close()}>
            <CloseIcon />
          </button>
        </header>
        {children}
      </div>
    </dialog>
  );
}
