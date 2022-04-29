/**
 * Provides a section of an accordion.
 *
 * To support a sliding animation, the component fixates the style's 
 * `height` value before expanding or collapsing the section and sets
 * it to zero while the section is collapsed. The value of `height` 
 * is unset when the section is expanded, thus allowing it to ajdust 
 * to the size of its content. 
 *
 * During the transition, the attribute `data-transitioning` is set
 * on the section.
 *
 * @module AashAccordionSection
 */
import { defineComponent, getCurrentInstance, onUnmounted, inject, computed,
    onMounted, ref, Ref, nextTick, watch } from 'vue'
import { Controller } from './AashAccordion'

/**
 * @classdesc
 * Generates the header and region of an accordion section.
 * 
 * @class AashAccordionSectionComponent
 * @param {Object} props the properties
 * @param {string} props.title the title, defaults to the empty string
 */
export default defineComponent({
    props: {
        title: { type: String, default: "" }
    },
    
    setup(props) {
        const globalProps = getCurrentInstance()!.appContext.config.globalProperties;
        
        const sectionId = globalProps.$aash.generateId();
        const headerType = inject('headerType');
        const buttonType = inject('buttonType');
        const panelType = inject('panelType');
        const panelClass = inject('panelClass');
        const theTitle = props.title;

        const ctrl: Controller = inject('controller')!;

        const index = ctrl.addSection(sectionId);
        
        const isExpanded = computed(() => {
            return ctrl.isExpanded(index);
        });
        
        const panel: Ref<HTMLElement | null> = ref(null);
 
        enum TransitioningState { Idle, FixateHeight, 
            ZeroHeight, ZeroHeightReached };
        
        const transitioningState = ref(ctrl.isExpanded(index) 
            ? TransitioningState.Idle : TransitioningState.ZeroHeightReached);
        
        const isTransitioning = computed(() => {
            return transitioningState.value != TransitioningState.Idle
                && transitioningState.value != TransitioningState.ZeroHeightReached;
        });
        
        watch (() => ctrl.isExpanded(index), () => {
            if (!panel.value) {
                return null;
            }
            let style = window.getComputedStyle(panel.value);
            if (style.transition === "" && style.animation === ""
                && style.animationName ==="") {
                return null;
            }
            
            if (ctrl.isExpanded(index)) {
                // Start expanding
                transitioningState.value = TransitioningState.FixateHeight;
                let handler = function (this: HTMLElement, e: Event) {
                    panel.value!.removeEventListener("transitionend", handler);
                    if (ctrl.isExpanded(index)) {
                        // Make sure that we still want to achieve this
                        transitioningState.value = TransitioningState.Idle;
                    }
                };
                panel.value.addEventListener("transitionend", handler); 
            } else {
                // Start hiding
                transitioningState.value = TransitioningState.FixateHeight;
                window.requestAnimationFrame (() => {
                    window.requestAnimationFrame(() => {
                        transitioningState.value = TransitioningState.ZeroHeight;
                        let handler = function (this: HTMLElement, e: Event) {
                            panel.value!.removeEventListener("transitionend", handler);
                            if (!ctrl.isExpanded(index)) {
                                // Make sure that we still want to achieve this
                                transitioningState.value = TransitioningState.ZeroHeightReached;
                            }
                        };
                        panel.value!.addEventListener("transitionend", handler); 
                    });
                });
            }
        })
        
        const onClick = (event: Event) => {
            ctrl.toggleExpanded(index);
        }
        
        const height = computed(() => {
            if (transitioningState.value == TransitioningState.FixateHeight) {
                let style = window.getComputedStyle(panel.value!);
                if (style.display !== "none") {
                    return panel.value!.scrollHeight + 'px'
                } else {
                    let position = panel.value!.style.position;
                    let visibility = panel.value!.style.visibility;
                    let display = panel.value!.style.display;
                    panel.value!.style.visibility = 'hidden';
                    panel.value!.style.position = 'absolute';
                    panel.value!.style.display = 'block';
                    let result = panel.value!.scrollHeight + 'px';
                    panel.value!.style.display = display;
                    panel.value!.style.position = position;
                    panel.value!.style.visibility = visibility;
                    return result;
                }
            }
            if (transitioningState.value == TransitioningState.ZeroHeight
                || transitioningState.value == TransitioningState.ZeroHeightReached) {
                return "0";
            }
            return null;
        });
        
        const onKey = (event: KeyboardEvent) => {
            if (event.key === "ArrowDown") {
                ctrl.selectNext(index);
                return;
            }
            if (event.key === "ArrowUp") {
                ctrl.selectPrev(index);
                return;
            }
            if (event.key === "Home") {
                ctrl.selectFirst();
                return;
            }
            if (event.key === "End") {
                ctrl.selectLast();
                return;
            }
        }
        
        return { sectionId, headerType, buttonType, panelType, panelClass,
            theTitle, ctrl, isExpanded, index, onClick, onKey, panel,
            height, isTransitioning };
    }
});
