import { onMounted, onBeforeUnmount, ref } from 'vue';
import * as echarts from 'echarts';
const lineRef = ref(null);
const pieRef = ref(null);
let lineChart = null;
let pieChart = null;
onMounted(() => {
    if (lineRef.value) {
        lineChart = echarts.init(lineRef.value);
        lineChart.setOption({
            xAxis: { type: 'category', data: ['08:00', '09:00', '10:00', '11:00'] },
            yAxis: { type: 'value' },
            series: [{ type: 'line', data: [120, 160, 150, 180], smooth: true }]
        });
    }
    if (pieRef.value) {
        pieChart = echarts.init(pieRef.value);
        pieChart.setOption({
            series: [
                {
                    type: 'pie',
                    radius: '60%',
                    data: [
                        { name: '汽车', value: 62 },
                        { name: '非机动车', value: 28 },
                        { name: '行人', value: 10 }
                    ]
                }
            ]
        });
    }
});
onBeforeUnmount(() => {
    lineChart?.dispose();
    pieChart?.dispose();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "lineRef",
    ...{ class: "chart" },
});
/** @type {typeof __VLS_ctx.lineRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "pieRef",
    ...{ class: "chart" },
});
/** @type {typeof __VLS_ctx.pieRef} */ ;
/** @type {__VLS_StyleScopedClasses['chart']} */ ;
/** @type {__VLS_StyleScopedClasses['chart']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            lineRef: lineRef,
            pieRef: pieRef,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
