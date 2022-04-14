/**
 * Provides a section of an accordion.
 *
 * @module AashAccordionSection
 */
import { defineComponent, getCurrentInstance, onUnmounted, inject, computed
    } from 'vue'
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
        
        const isExpanded = () => {
            return ctrl.isExpanded(index);
        }
        
        const onClick = (event: Event) => {
            ctrl.toggleExpanded(index);
        }
        
        const onKey = (event: KeyboardEvent) => {
/*            if (event.key == "Enter" || event.key === " ") {
                onClick(event);
                return;
            }
*/            if (event.key === "ArrowDown") {
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
            theTitle, ctrl, isExpanded, index, onClick, onKey };
    }
});
