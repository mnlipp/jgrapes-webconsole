import {Chart, registerables } from '../chart.js';
import '../chartjs-adapter-date-std.js';

Chart.register(...registerables);

// export * from '../../../../../../../../node_modules/chart.js/dist/chart';
export * from '../chart.js'
export default Chart;
