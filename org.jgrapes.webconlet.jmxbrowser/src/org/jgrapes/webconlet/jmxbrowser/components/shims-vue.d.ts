declare module "*.vue" {
    import { defineComponent } from "@Vue";
    const Component: ReturnType<typeof defineComponent>;
    export default Component;
}
