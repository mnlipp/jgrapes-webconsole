/**
 * Provides some utility functions.
 *
 * @module AashUtil
 */

import { Ref, isRef, onMounted, onBeforeUnmount } from 'vue'

/**
 * Ensures (using `onMount` and `onBeforeUnmount`) that the given
 * api is made available as property of the given HTMLElement.
 * @param dom the element from the DOM tree
 * @param api the API to make available
 */
function provideApi (dom: Ref<null> | Ref<HTMLElement> | HTMLElement, 
        api: Object): void {
    onMounted (() => {
        if (isRef(dom)) {
            /* When `onMounted` is evaluated, a ref is usually bound to the
             * DOM (using a `ref` attribute). However, when using a template
             * recursively, the ref may only be bound for the top level usage. */
            if (!dom.value) {
                return;
            }
            dom = dom.value;
        }
        (<any>dom).__componentApi__ = api;
    });
    
    onBeforeUnmount (() => {
        if (isRef(dom)) {
            if (!dom.value) {
                return;
            }
            dom = dom.value!;
        }
        delete (<any>dom).__componentApi__;
    })
}

/**
 * Retrieves a provided API from the given HTMLElement. The function is
 * "null-safe", i.e. if the argument is `null`, it returns `null`.
 *
 * @param dom the element from the DOM tree with the api property set
 *  by {@link provideApi}.
 */
function getApi<T> (dom: HTMLElement | null): T | null {
    if (!dom) {
        return null;
    }
    return <T>((<any>dom).__componentApi__);
}

export { provideApi, getApi }