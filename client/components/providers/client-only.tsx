"use client";

import * as React from "react";

/**
 * Delays rendering the application tree until the browser has mounted it.
 * This opts the app out of server-rendered markup and its corresponding
 * hydration pass while keeping the document shell available to the browser.
 */
export function ClientOnly({ children }: { children: React.ReactNode }) {
  const mounted = React.useSyncExternalStore(
    () => () => {},
    () => true,
    () => false,
  );

  return mounted ? children : null;
}
