/**
 * Provides a section of an accordion.
 *
 * @module AashAccordionSection
 */
import { defineComponent, getCurrentInstance, onUnmounted, inject, computed,
    onMounted, ref, Ref, nextTick, watch } from "vue";
import { Controller } from "./AashAccordion";

/**
 * @classdesc
 * Generates the header and region of an accordion section.
 * 
 * The component supports a sliding animation by setting the `height`
 * attribute of the region on two occasions if a transition for 
 * `height` is defined for the region. 
 *
 * When the region is to be expanded, the component calculates the
 * (expected) height of the region and sets its style's `height` to that
 * value. When the transition has ended, the explicit `height` value is
 * removed again, thus allowing the section to adjust to the size of its 
 * content if that content changes.
 *
 * When the region is to be collapsed, the component calculates the
 * current height of the region and assigns the result to the
 * style's `height` for one animation frame. Together with a CSS rule
 * that requests `height: 0` if the region is hidden, this triggers
 * a transition from the calculated height to 0.
 *
 * During either transition, the attribute `data-transitioning` is set
 * on the region.
 *
 * Note that adding padding to the section breaks the transition. Therefore,
 * an extra `div` is generated as child of the section that contains the
 * slot content. Use this `div` to add any padding.
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
 
        enum TransitioningState { Expanded, FixateHeight, 
            Collapsing, Collapsed }
        
        const transitioningState = ref(ctrl.isExpanded(index) 
            ? TransitioningState.Expanded : TransitioningState.Collapsed);
        
        const isTransitioning = computed(() => {
            return transitioningState.value !== TransitioningState.Expanded
                && transitioningState.value !== TransitioningState.Collapsed;
        });
        
        watch (() => ctrl.isExpanded(index), () => {
            if (!panel.value) {
                return null;
            }
            let style = window.getComputedStyle(panel.value);
            if (!style.transitionProperty.split(",")
                    .map((s) => s.trim()).includes("height")) {
                return null;
            }
            
            if (ctrl.isExpanded(index)) {
                // Prepare for transition and start expanding
                let handler = function (this: HTMLElement, e: TransitionEvent) {
                    if (e.propertyName !== "height"
                        || e.propertyName !== "height") {
                        return;
                    }
                    panel.value!.removeEventListener("transitionend", handler);
                    // Make sure that we still want to achieve this
                    if (ctrl.isExpanded(index)) {
                        transitioningState.value = TransitioningState.Expanded;
                    }
                };
                panel.value.addEventListener("transitionend", handler); 
                transitioningState.value = TransitioningState.FixateHeight;
            } else {
                // Prepare for transition and start hiding
                window.requestAnimationFrame(() => {
                    // It takes two frames for our explicit height to become
                    // effective. 
                    window.requestAnimationFrame(() => {
                        let handler = function (this: HTMLElement, e: TransitionEvent) {
                            if (e.target !== panel.value
                                || e.propertyName !== "height") {
                                return;
                            }
                            panel.value!.removeEventListener("transitionend", handler);
                            // Make sure that we still want to achieve this
                            if (!ctrl.isExpanded(index)) {
                                transitioningState.value = TransitioningState.Collapsed;
                            }
                        };
                        panel.value!.addEventListener("transitionend", handler); 
                        transitioningState.value = TransitioningState.Collapsing;
                    });
                });
                transitioningState.value = TransitioningState.FixateHeight;
            }
        });
        
        const onClick = (event: Event) => {
            ctrl.toggleExpanded(index);
        };
        
        const height = computed(() => {
            if (transitioningState.value == TransitioningState.FixateHeight) {
                let result: string | null = null;
                let style = window.getComputedStyle(panel.value!);
                if (style.display !== "none") {
                    result = panel.value!.scrollHeight + "px";
                } else {
                    let position = panel.value!.style.position;
                    let visibility = panel.value!.style.visibility;
                    let display = panel.value!.style.display;
                    panel.value!.style.visibility = "hidden";
                    panel.value!.style.position = "absolute";
                    panel.value!.style.display = "block";
                    result = panel.value!.scrollHeight + "px";
                    panel.value!.style.display = display;
                    panel.value!.style.position = position;
                    panel.value!.style.visibility = visibility;
                }
                return result;
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
