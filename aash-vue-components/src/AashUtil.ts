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
function provideApi (dom: Ref<HTMLElement | null> | HTMLElement, 
        api: Object): void {
    onMounted (() => {
        if (isRef(dom)) {
            dom = dom.value!;
        }
        (<any>dom).__componentApi__ = api;
    });
    
    onBeforeUnmount (() => {
        if (isRef(dom)) {
            dom = dom.value!;
        }
        delete (<any>dom).__componentApi__;
    })
}

/**
 * Retrieves a provided API from the given HTMLElement.
 * @param dom the element from the DOM tree
 */
function getApi<T> (dom: HTMLElement): T | null {
    return <T>((<any>dom).__componentApi__);
}

export { provideApi, getApi }