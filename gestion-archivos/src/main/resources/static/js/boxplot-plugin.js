(function () {
    const boxplotPlugin = {
        id: 'customBoxplot',
        afterDatasetsDraw(chart) {
            const { ctx, scales, canvas } = chart;
            let boxplotData = chart.boxplotData || [];
            if (!boxplotData.length && chart.config && chart.config.data && Array.isArray(chart.config.data.datasets)) {
                const dataset = chart.config.data.datasets[0];
                if (dataset && Array.isArray(dataset.data) && dataset.data.length > 0 && typeof dataset.data[0] === 'object' && dataset.data[0] !== null &&
                    'min' in dataset.data[0] && 'q1' in dataset.data[0] && 'median' in dataset.data[0] && 'q3' in dataset.data[0] && 'max' in dataset.data[0]) {
                    boxplotData = dataset.data;
                }
            }

            if (!boxplotData.length || !scales || !scales.x || !scales.y) {
                return;
            }

            const xScale = scales.x;
            const yScale = scales.y;
            const labelCount = Array.isArray(chart.config.data.labels) ? chart.config.data.labels.length : boxplotData.length;
            const defaultWidth = Math.max(1, (xScale.getPixelForValue(1) - xScale.getPixelForValue(0)) * 0.35);

            // Almacenar datos de hitbox para tooltips
            chart.boxplotHitBoxes = [];

            ctx.save();
            ctx.lineWidth = 2;

            boxplotData.forEach((item, index) => {
                const centerX = xScale.getPixelForValue(index);
                const barWidth = defaultWidth;
                const left = centerX - barWidth / 2;
                const right = centerX + barWidth / 2;

                const minY = yScale.getPixelForValue(item.min);
                const q1Y = yScale.getPixelForValue(item.q1);
                const medianY = yScale.getPixelForValue(item.median);
                const q3Y = yScale.getPixelForValue(item.q3);
                const maxY = yScale.getPixelForValue(item.max);
                const boxTop = Math.min(q1Y, q3Y);
                const boxBottom = Math.max(q1Y, q3Y);
                const boxHeight = Math.max(1, boxBottom - boxTop);

                // Almacenar hitbox para esta caja de bigotes
                chart.boxplotHitBoxes.push({
                    left,
                    right,
                    top: Math.min(minY, maxY),
                    bottom: Math.max(minY, maxY),
                    data: item,
                    label: chart.config.data.labels[index]
                });

                ctx.strokeStyle = item.color || '#000000';
                ctx.lineWidth = 2;
                
                // Dibujar los bigotes (whiskers)
                ctx.beginPath();
                ctx.moveTo(centerX, minY);
                ctx.lineTo(centerX, q1Y);
                ctx.moveTo(centerX, q3Y);
                ctx.lineTo(centerX, maxY);
                // Líneas horizontales en min y max
                ctx.moveTo(left, minY);
                ctx.lineTo(right, minY);
                ctx.moveTo(left, maxY);
                ctx.lineTo(right, maxY);
                ctx.stroke();
                
                // Dibujar la parte superior de la caja (Q1 a mediana) con color turquesa
                const upperColor = item.upperColor || '#4ECDC4';
                ctx.fillStyle = upperColor;
                
                // Parte superior de la caja
                ctx.beginPath();
                const upperBoxTop = Math.min(q1Y, medianY);
                const upperBoxBottom = Math.max(q1Y, medianY);
                const upperBoxHeight = Math.max(1, upperBoxBottom - upperBoxTop);
                ctx.rect(left, upperBoxTop, right - left, upperBoxHeight);
                ctx.fill();
                ctx.stroke();
                
                // Dibujar la parte inferior de la caja (mediana a Q3) con color rojo
                const lowerColor = item.lowerColor || '#FF6B6B';
                ctx.fillStyle = lowerColor;
                
                // Parte inferior de la caja
                ctx.beginPath();
                const lowerBoxTop = Math.min(medianY, q3Y);
                const lowerBoxBottom = Math.max(medianY, q3Y);
                const lowerBoxHeight = Math.max(1, lowerBoxBottom - lowerBoxTop);
                ctx.rect(left, lowerBoxTop, right - left, lowerBoxHeight);
                ctx.fill();
                ctx.stroke();
                ctx.moveTo(left, medianY);
                ctx.lineTo(right, medianY);
                ctx.stroke();
            });

            ctx.restore();
            
            // Configurar event listeners para tooltips
            setupBoxplotTooltips(chart, canvas);
        }
    };

    function setupBoxplotTooltips(chart, canvas) {
        if (chart._boxplotTooltipsSetup) {
            return; // Ya configurado
        }
        chart._boxplotTooltipsSetup = true;

        let tooltipElement = null;

        canvas.addEventListener('mousemove', (e) => {
            const rect = canvas.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            let hoveredBox = null;
            if (chart.boxplotHitBoxes) {
                for (const box of chart.boxplotHitBoxes) {
                    if (x >= box.left && x <= box.right && y >= box.top && y <= box.bottom) {
                        hoveredBox = box;
                        break;
                    }
                }
            }

            if (hoveredBox) {
                if (!tooltipElement) {
                    tooltipElement = document.createElement('div');
                    tooltipElement.className = 'boxplot-tooltip';
                    tooltipElement.style.cssText = `
                        position: fixed;
                        background: rgba(0, 0, 0, 0.8);
                        color: white;
                        padding: 8px 12px;
                        border-radius: 4px;
                        font-size: 12px;
                        pointer-events: none;
                        z-index: 1000;
                        font-family: monospace;
                        white-space: nowrap;
                    `;
                    document.body.appendChild(tooltipElement);
                }

                const data = hoveredBox.data;
                const tooltipText = `${hoveredBox.label} - Mín: ${data.min.toFixed(2)} | Q1: ${data.q1.toFixed(2)} | Med: ${data.median.toFixed(2)} | Q3: ${data.q3.toFixed(2)} | Máx: ${data.max.toFixed(2)}`;
                tooltipElement.textContent = tooltipText;
                tooltipElement.style.left = (e.clientX + 10) + 'px';
                tooltipElement.style.top = (e.clientY + 10) + 'px';
                tooltipElement.style.display = 'block';
                canvas.style.cursor = 'pointer';
            } else {
                if (tooltipElement) {
                    tooltipElement.style.display = 'none';
                }
                canvas.style.cursor = 'default';
            }
        });

        canvas.addEventListener('mouseleave', () => {
            if (tooltipElement) {
                tooltipElement.style.display = 'none';
            }
            canvas.style.cursor = 'default';
        });
    }

    if (typeof window !== 'undefined' && window.Chart) {
        window.Chart.register(boxplotPlugin);
    }
    if (typeof globalThis !== 'undefined' && globalThis.Chart) {
        globalThis.Chart.register(boxplotPlugin);
    }
})();
