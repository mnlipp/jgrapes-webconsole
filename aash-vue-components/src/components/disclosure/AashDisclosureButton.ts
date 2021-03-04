/**
 * Provides an element that controlls the visibility of another element.
 *
 * @module AashDisclosureButton
 */
import { defineComponent, getCurrentInstance, onUnmounted } from 'vue'

/**
 * @classdesc
 * Generates an element (a `button` by default) that toggles the
 * the visibility of another element when clicked.
 *
 * Example:
 * ```html
 * <button type="button" data-aash-role="disclosure-button"
 *   aria-expanded="true" 
 *   aria-controls="onlyShownWhenDisclosed">Disclose</button>
 * ```
 * 
 * @class AashDisclosureButton
 * @param {Object} props the properties
 * @param {string} props.idRef the id of the controlled element
 */
export default defineComponent({
    props: {
        idRef: { type: String, required: true },
        type: { type: String, default: "button" },
        onShow: { type: Function, default: null },
        onHide: { type: Function, default: null },
        onToggle: { type: Function, default: null },
    },
    
    setup(props) {
        let globalProps = getCurrentInstance()!.appContext.config.globalProperties;
        const disclosed = globalProps.$aash.disclosureData(props.idRef);
        
        const toggleDisclosed = () => {
            disclosed.value = !disclosed.value;
            if (props.onToggle) {
                props.onToggle.call(this, disclosed.value);
            }
            if (disclosed.value) {
                if (props.onShow) {
                    props.onShow.call(this);
                }
            } else {
                if (props.onHide) {
                    props.onHide.call(this);
                }
            }
        }
  
        onUnmounted(() => {
            globalProps.$aash.removeDisclose(props.idRef);
        });
        
        return { toggleDisclosed, disclosed };
    }
});
