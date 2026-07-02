(function () {
    const boxplotPlugin = {
        id: 'customBoxplot',
        afterDatasetsDraw(chart) {
            const { ctx, scales } = chart;
            const boxplotData = chart.boxplotData || [];

            if (!boxplotData.length || !scales.x || !scales.y) {
                return;
            }

            const xScale = scales.x;
            const yScale = scales.y;
            const categoryWidth = Math.max(1, (xScale.getPixelForValue(1) - xScale.getPixelForValue(0)) * 0.35);

            ctx.save();

            boxplotData.forEach((item, index) => {
                const centerX = xScale.getPixelForValue(index);
                const left = centerX - categoryWidth / 2;
                const right = centerX + categoryWidth / 2;

                const minY = yScale.getPixelForValue(item.min);
                const q1Y = yScale.getPixelForValue(item.q1);
                const medianY = yScale.getPixelForValue(item.median);
                const q3Y = yScale.getPixelForValue(item.q3);
                const maxY = yScale.getPixelForValue(item.max);
                const boxTop = Math.min(q1Y, q3Y);
                const boxHeight = Math.abs(q3Y - q1Y);

                ctx.strokeStyle = item.color || '#3498db';
                ctx.lineWidth = 2;
                ctx.fillStyle = item.fillColor || 'rgba(52, 152, 219, 0.25)';

                ctx.beginPath();
                ctx.moveTo(centerX, minY);
                ctx.lineTo(centerX, q1Y);
                ctx.moveTo(centerX, q3Y);
                ctx.lineTo(centerX, maxY);
                ctx.stroke();

                ctx.fillRect(left, boxTop, right - left, boxHeight);
                ctx.strokeRect(left, boxTop, right - left, boxHeight);

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
