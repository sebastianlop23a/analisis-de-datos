(function () {
    const boxplotPlugin = {
        id: 'customBoxplot',
        afterDatasetsDraw(chart) {
            const { ctx, scales } = chart;
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

                ctx.strokeStyle = item.color || '#3498db';
                ctx.fillStyle = item.fillColor || 'rgba(52, 152, 219, 0.25)';

                ctx.beginPath();
                ctx.moveTo(centerX, minY);
                ctx.lineTo(centerX, q1Y);
                ctx.moveTo(centerX, q3Y);
                ctx.lineTo(centerX, maxY);
                ctx.stroke();

                ctx.beginPath();
                ctx.rect(left, boxTop, right - left, boxHeight);
                ctx.fill();
                ctx.stroke();

                ctx.beginPath();
                ctx.moveTo(left, medianY);
                ctx.lineTo(right, medianY);
                ctx.stroke();
            });

            ctx.restore();
        }
    };

    if (typeof window !== 'undefined' && window.Chart) {
        window.Chart.register(boxplotPlugin);
    }
    if (typeof globalThis !== 'undefined' && globalThis.Chart) {
        globalThis.Chart.register(boxplotPlugin);
    }
})();
