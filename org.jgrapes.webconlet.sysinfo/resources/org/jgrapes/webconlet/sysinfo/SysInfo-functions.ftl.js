/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016, 2022  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

import JGConsole from "../../console-base-resource/jgconsole.js"
import { Chart } from "../../page-resource/chart.js/dist/chart.esm.js";

let orgJGrapesConletsSysInfo = {};
window.orgJGrapesConletsSysInfo = orgJGrapesConletsSysInfo;

$("body").on("click", ".jgrapes-conlet-sysinfo-view .GarbageCollection-action",
        function(event) {
    let conletId = $(this).closest("[data-conlet-id]").attr("data-conlet-id");
    JGConsole.notifyConletModel(conletId, "garbageCollection");
})

let timeData = [];
let maxMemoryData = [];
let totalMemoryData = [];
let usedMemoryData = [];

JGConsole.registerConletFunction(
        "org.jgrapes.webconlet.sysinfo.SysInfoConlet",
        "updateMemorySizes", function(conletId, time, 
            maxMemory, totalMemory, usedMemory) {
            if (timeData.length >= 301) {
                timeData.shift();
                maxMemoryData.shift();
                totalMemoryData.shift();
                usedMemoryData.shift();
            }
            timeData.push(time);
            maxMemoryData.push(maxMemory);
            totalMemoryData.push(totalMemory);
            usedMemoryData.push(usedMemory);
            let maxFormatted = "";
            let totalFormatted = "";
            let usedFormatted = "";
            let conlet = JGConsole.findConletPreview(conletId);
            let lang = 'en';
            if (conlet) {
                conlet = $(conlet);
                lang = conlet.closest('[lang]').attr('lang') || 'en'
                maxFormatted = JGConsole.formatMemorySize(maxMemory, 1, lang);
                totalFormatted = JGConsole.formatMemorySize(totalMemory, 1, lang);
                usedFormatted = JGConsole.formatMemorySize(usedMemory, 1, lang);
                let col = conlet.find(".maxMemory");
                col.html(maxFormatted);
                col = conlet.find(".totalMemory");
                col.html(totalFormatted);
                col = conlet.find(".usedMemory");
                col.html(usedFormatted);
            }
            conlet = JGConsole.findConletView(conletId);
            if (conlet) {
                conlet = $(conlet);
                let col = conlet.find(".maxMemory");
                col.html(maxFormatted);
                col = conlet.find(".totalMemory");
                col.html(totalFormatted);
                col = conlet.find(".usedMemory");
                col.html(usedFormatted);
                let chartCanvas = conlet.find(".memoryChart");
                if (conlet.find(".memoryChart").parent(":hidden").length === 0) {
                    let chart = chartCanvas.data('chartjs-chart');
                    if (chart) {
                        chart.update(0);
                    }
                }
            }
        });

orgJGrapesConletsSysInfo.initMemoryChart = function(content) {
    let chartCanvas = $(content).find(".memoryChart");
    let ctx = chartCanvas[0].getContext('2d');
    let lang = chartCanvas.closest('[lang]').attr('lang') || 'en'
    let chart = new Chart(ctx, {
        // The type of chart we want to create
        type: 'line',

        // The data for our datasets
        data: {
            labels: timeData,
            datasets: [{
                lineTension: 0,
                fill: false,
                borderWidth: 2,
                pointRadius: 1,
                borderColor: "rgba(255,0,0,1)",
                label: '${_("maxMemory")}',
                data: maxMemoryData,
            },{
                lineTension: 0,
                fill: false,
                borderWidth: 2,
                pointRadius: 1,
                borderColor: "rgba(255,165,0,1)",
                label: '${_("totalMemory")}',
                data: totalMemoryData,
            },{
                lineTension: 0,
                fill: false,
                borderWidth: 2,
                pointRadius: 1,
                borderColor: "rgba(0,255,0,1)",
                label: '${_("usedMemory")}',
                data: usedMemoryData,
            }]
        },

        // Configuration options go here
        options: {
            animation: false,
            maintainAspectRatio: false,
            scales: {
                xAxes: {
                    distribution: 'linear',
                    type: 'time',
                    adapters: {
                        date: {
                            locale: chartCanvas.closest('[lang]').attr('lang') || 'en'
                        }
                    }
                },
                yAxes: {
                    ticks: {
                        callback: function(value, index, values) {
                            return JGConsole.formatMemorySize(value, 0, lang);
                        }
                    }
                }
            }
        }
    });
    chartCanvas.data('chartjs-chart', chart);
}

