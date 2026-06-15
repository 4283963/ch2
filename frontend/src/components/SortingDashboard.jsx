import React, { useRef, useEffect, useState, useCallback } from 'react';
import './SortingDashboard.css';

const SortingDashboard = () => {
  const canvasRef = useRef(null);
  const animationRef = useRef(null);
  const packagesRef = useRef([]);
  const sortersRef = useRef([]);
  const statsRef = useRef({ scanned: 0, sorted: 0 });
  const [wsConnected, setWsConnected] = useState(false);
  const [stats, setStats] = useState({ scanned: 0, sorted: 0 });
  const [monitorData, setMonitorData] = useState({
    systemStatus: 'NORMAL',
    currentQps: 0,
    queueSize: 0,
    avgLatency: 0,
    maxLatency: 0,
  });
  const wsRef = useRef(null);
  const packageIdRef = useRef(0);
  const monitorRef = useRef({
    systemStatus: 'NORMAL',
    qps: 0,
    queue: 0,
  });
  const [antiWearEnabled, setAntiWearEnabled] = useState(false);
  const antiWearRef = useRef({
    enabled: false,
    sorters: {},
  });

  const conveyorConfig = {
    line1: {
      y: 180,
      startX: 80,
      endX: 1100,
      height: 50,
      sorters: [
        { id: 101, x: 300, warehouse: '北京仓', code: 'BJ01', color: '#ef4444' },
        { id: 102, x: 500, warehouse: '上海仓', code: 'SH01', color: '#3b82f6' },
        { id: 103, x: 700, warehouse: '广州仓', code: 'GZ01', color: '#22c55e' },
        { id: 104, x: 900, warehouse: '深圳仓', code: 'SZ01', color: '#f59e0b' },
      ],
    },
    line2: {
      y: 420,
      startX: 80,
      endX: 1100,
      height: 50,
      sorters: [
        { id: 201, x: 300, warehouse: '成都仓', code: 'CD01', color: '#8b5cf6' },
        { id: 202, x: 550, warehouse: '武汉仓', code: 'WH01', color: '#ec4899' },
        { id: 203, x: 800, warehouse: '西安仓', code: 'XA01', color: '#14b8a6' },
      ],
    },
  };

  const initSorters = useCallback(() => {
    const sorters = [];
    Object.keys(conveyorConfig).forEach((lineKey) => {
      const line = conveyorConfig[lineKey];
      line.sorters.forEach((s) => {
        sorters.push({
          ...s,
          lineKey,
          lineY: line.y,
          active: false,
          activeTimer: 0,
        });
      });
    });
    sortersRef.current = sorters;
  }, []);

  const drawBackground = (ctx, width, height) => {
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(0, 0, width, height);

    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    for (let x = 0; x < width; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += 40) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }
  };

  const drawConveyor = (ctx, config, lineNum) => {
    const { y, startX, endX, height } = config;

    ctx.fillStyle = '#334155';
    ctx.fillRect(startX - 10, y - height / 2 - 5, endX - startX + 20, height + 10);

    ctx.fillStyle = '#475569';
    ctx.fillRect(startX, y - height / 2, endX - startX, height);

    ctx.fillStyle = '#64748b';
    for (let x = startX + 20; x < endX - 20; x += 30) {
      ctx.fillRect(x, y - height / 2 + 5, 15, height - 10);
    }

    ctx.fillStyle = '#f8fafc';
    ctx.font = 'bold 14px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`${lineNum}号线`, startX - 60, y + 5);

    ctx.fillStyle = '#22c55e';
    ctx.beginPath();
    ctx.arc(startX - 30, y - 25, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#94a3b8';
    ctx.font = '11px sans-serif';
    ctx.fillText('扫描枪', startX - 55, y - 10);
  };

  const drawSorter = (ctx, sorter) => {
    const { x, lineY, color, warehouse, code, active, activeTimer, id } = sorter;
    const isHeld = antiWearRef.current.sorters[id]?.isHeld || false;

    const binY = lineY + 80;
    const binWidth = 70;
    const binHeight = 50;

    if (isHeld) {
      const glowSize = 15 + Math.sin(Date.now() / 300) * 3;
      ctx.shadowColor = '#22c55e';
      ctx.shadowBlur = glowSize;
    }

    ctx.fillStyle = isHeld ? '#22c55e' : color;
    ctx.globalAlpha = isHeld ? 0.3 : 0.2;
    ctx.fillRect(x - binWidth / 2, binY, binWidth, binHeight);
    ctx.globalAlpha = 1;

    ctx.strokeStyle = isHeld ? '#22c55e' : color;
    ctx.lineWidth = isHeld ? 3 : 2;
    ctx.strokeRect(x - binWidth / 2, binY, binWidth, binHeight);

    ctx.fillStyle = isHeld ? '#22c55e' : color;
    ctx.font = 'bold 12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(warehouse, x, binY + 30);
    ctx.font = '10px sans-serif';
    ctx.fillStyle = '#94a3b8';
    ctx.fillText(code, x, binY + 45);

    const baffleHeight = 35;
    const baffleY = lineY - baffleHeight / 2;

    ctx.fillStyle = '#475569';
    ctx.fillRect(x - 5, lineY - baffleHeight / 2 - 10, 10, 10);

    ctx.shadowBlur = 0;

    if (active || isHeld) {
      let swingAngle;
      if (isHeld && !active) {
        swingAngle = Math.PI / 3;
      } else {
        swingAngle = Math.min((activeTimer / 20) * Math.PI / 3, Math.PI / 3);
      }
      ctx.save();
      ctx.translate(x, lineY - baffleHeight / 2);
      ctx.rotate(swingAngle);
      ctx.fillStyle = isHeld ? '#22c55e' : color;
      if (isHeld) {
        ctx.shadowColor = '#22c55e';
        ctx.shadowBlur = 10 + Math.sin(Date.now() / 200) * 5;
      }
      ctx.fillRect(-3, 0, 6, baffleHeight);
      ctx.shadowBlur = 0;
      ctx.restore();
    } else {
      ctx.fillStyle = '#64748b';
      ctx.fillRect(x - 3, lineY - baffleHeight / 2, 6, baffleHeight);
    }

    if (isHeld) {
      ctx.fillStyle = '#22c55e';
      ctx.font = 'bold 9px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('● 保持中', x, lineY - baffleHeight / 2 - 15);
    }

    ctx.fillStyle = '#94a3b8';
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(`#${sorter.id}`, x, lineY + 35);
  };

  const drawPackage = (ctx, pkg) => {
    const { x, y, color, barcode, size } = pkg;
    const w = size || 30;
    const h = size || 25;

    ctx.fillStyle = 'rgba(0,0,0,0.3)';
    ctx.fillRect(x - w / 2 + 3, y - h / 2 + 3, w, h);

    ctx.fillStyle = color;
    ctx.fillRect(x - w / 2, y - h / 2, w, h);

    ctx.strokeStyle = 'rgba(255,255,255,0.5)';
    ctx.lineWidth = 1;
    ctx.strokeRect(x - w / 2, y - h / 2, w, h);

    ctx.fillStyle = '#ffffff';
    ctx.font = '8px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(barcode, x, y + 3);
  };

  const drawStats = (ctx, width) => {
    const statsBarY = 60;

    ctx.fillStyle = '#1e293b';
    ctx.fillRect(0, 0, width, statsBarY);

    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(0, statsBarY);
    ctx.lineTo(width, statsBarY);
    ctx.stroke();

    const status = monitorRef.current.systemStatus || 'NORMAL';
    let statusColor = '#22c55e';
    let statusText = '正常';
    if (status === 'WARN') {
      statusColor = '#f59e0b';
      statusText = '警告';
    } else if (status === 'ALARM') {
      statusColor = '#ef4444';
      statusText = '告警';
    }

    ctx.fillStyle = statusColor;
    ctx.fillRect(0, 0, 5, statsBarY);

    ctx.fillStyle = '#f8fafc';
    ctx.font = 'bold 20px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('分拣线联控系统', 20, 38);

    ctx.fillStyle = statusColor;
    ctx.font = 'bold 12px sans-serif';
    ctx.fillText(`● ${statusText}`, 200, 38);

    if (monitorRef.current.qps > 0) {
      ctx.fillStyle = '#94a3b8';
      ctx.font = '12px sans-serif';
      ctx.fillText(`QPS: ${monitorRef.current.qps}`, 280, 38);
    }
    if (monitorRef.current.queue > 0) {
      ctx.fillStyle = monitorRef.current.queue > 20 ? '#ef4444' : '#f59e0b';
      ctx.font = '12px sans-serif';
      ctx.fillText(`队列: ${monitorRef.current.queue}`, 380, 38);
    }

    const statsX = width - 300;
    const statSpacing = 100;

    ctx.fillStyle = '#94a3b8';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('今日扫描', statsX, 25);
    ctx.fillStyle = '#22c55e';
    ctx.font = 'bold 24px sans-serif';
    ctx.fillText(statsRef.current.scanned, statsX, 50);

    ctx.fillStyle = '#94a3b8';
    ctx.font = '12px sans-serif';
    ctx.fillText('今日分拣', statsX + statSpacing, 25);
    ctx.fillStyle = '#3b82f6';
    ctx.font = 'bold 24px sans-serif';
    ctx.fillText(statsRef.current.sorted, statsX + statSpacing, 50);

    ctx.fillStyle = '#94a3b8';
    ctx.font = '12px sans-serif';
    ctx.fillText('连接状态', statsX + statSpacing * 2, 25);
    if (wsConnected) {
      ctx.fillStyle = '#22c55e';
      ctx.font = 'bold 14px sans-serif';
      ctx.fillText('● 已连接', statsX + statSpacing * 2, 50);
    } else {
      ctx.fillStyle = '#ef4444';
      ctx.font = 'bold 14px sans-serif';
      ctx.fillText('● 未连接', statsX + statSpacing * 2, 50);
    }
  };

  const drawDockIndicators = (ctx) => {
    ctx.fillStyle = '#94a3b8';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'center';

    Object.keys(conveyorConfig).forEach((lineKey, idx) => {
      const line = conveyorConfig[lineKey];
      line.sorters.forEach((sorter) => {
        ctx.fillText('▼ 集装箱', sorter.x, line.y + 150);
      });
    });
  };

  const addPackage = useCallback((sortingResult) => {
    const lineKey = sortingResult.conveyorLine === 2 ? 'line2' : 'line1';
    const line = conveyorConfig[lineKey];
    if (!line) return;

    const sorter = line.sorters.find((s) => s.id === sortingResult.sorterId);
    const color = sorter ? sorter.color : '#94a3b8';

    const pkg = {
      id: packageIdRef.current++,
      barcode: sortingResult.barcode,
      x: line.startX,
      y: line.y,
      color,
      speed: 2.5,
      lineKey,
      targetSorterId: sortingResult.sorterId,
      state: 'moving',
      size: 28,
      fallY: 0,
      fallSpeed: 0,
    };

    packagesRef.current.push(pkg);

    statsRef.current.scanned += 1;
    setStats({ ...statsRef.current });
  }, []);

  const activateSorter = useCallback((sorterId) => {
    const sorter = sortersRef.current.find((s) => s.id === sorterId);
    if (sorter) {
      sorter.active = true;
      sorter.activeTimer = 20;
    }
  }, []);

  const updatePackages = useCallback(() => {
    const packages = packagesRef.current;

    for (let i = packages.length - 1; i >= 0; i--) {
      const pkg = packages[i];
      const line = conveyorConfig[pkg.lineKey];

      if (!line) {
        packages.splice(i, 1);
        continue;
      }

      if (pkg.state === 'moving') {
        pkg.x += pkg.speed;

        const sorter = line.sorters.find((s) => s.id === pkg.targetSorterId);
        if (sorter && pkg.x >= sorter.x - 5 && pkg.x <= sorter.x + 5) {
          pkg.state = 'falling';
          pkg.fallStartX = pkg.x;
          pkg.fallStartY = pkg.y;
          pkg.fallSpeed = 2;
          activateSorter(sorter.id);

          statsRef.current.sorted += 1;
          setStats({ ...statsRef.current });
        }

        if (pkg.x > line.endX + 50) {
          packages.splice(i, 1);
        }
      } else if (pkg.state === 'falling') {
        pkg.fallSpeed += 0.5;
        pkg.fallY += pkg.fallSpeed;
        pkg.x = pkg.fallStartX + (Math.random() - 0.5) * 5;

        if (pkg.fallY > 120) {
          packages.splice(i, 1);
        }
      }
    }
  }, [activateSorter]);

  const updateSorters = useCallback(() => {
    sortersRef.current.forEach((sorter) => {
      if (sorter.active && sorter.activeTimer > 0) {
        sorter.activeTimer -= 1;
        if (sorter.activeTimer <= 0) {
          sorter.active = false;
        }
      }
    });
  }, []);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    drawBackground(ctx, width, height);
    drawStats(ctx, width);

    Object.keys(conveyorConfig).forEach((lineKey, idx) => {
      drawConveyor(ctx, conveyorConfig[lineKey], idx + 1);
    });

    sortersRef.current.forEach((sorter) => {
      drawSorter(ctx, sorter);
    });

    drawDockIndicators(ctx);

    packagesRef.current.forEach((pkg) => {
      if (pkg.state === 'falling') {
        const displayPkg = {
          ...pkg,
          y: pkg.fallStartY + pkg.fallY,
          x: pkg.x,
        };
        drawPackage(ctx, displayPkg);
      } else {
        drawPackage(ctx, pkg);
      }
    });
  }, []);

  const gameLoop = useCallback(() => {
    updatePackages();
    updateSorters();
    draw();
    animationRef.current = requestAnimationFrame(gameLoop);
  }, [updatePackages, updateSorters, draw]);

  const fetchAntiWearStatus = useCallback(() => {
    fetch('/api/antiwear/status')
      .then((res) => res.json())
      .then((data) => {
        antiWearRef.current.enabled = data.enabled || false;
        antiWearRef.current.sorters = data.sorters || {};
        setAntiWearEnabled(data.enabled || false);
      })
      .catch((err) => {
        console.error('获取减磨状态失败:', err);
      });
  }, []);

  useEffect(() => {
    initSorters();
    gameLoop();

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [initSorters, gameLoop]);

  useEffect(() => {
    const connectWebSocket = () => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.hostname || 'localhost';
      const wsUrl = `${protocol}//${host}:8080/ws/sorting`;

      try {
        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;

        ws.onopen = () => {
          console.log('WebSocket 已连接');
          setWsConnected(true);
          fetchAntiWearStatus();
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'MONITOR' && data.data) {
              monitorRef.current = {
                systemStatus: data.data.status,
                qps: data.data.qps,
                queue: data.data.queueSize,
              };
              setMonitorData({
                systemStatus: data.data.status,
                currentQps: data.data.qps,
                queueSize: data.data.queueSize,
                avgLatency: data.data.avgLatency || 0,
                maxLatency: data.data.maxLatency || 0,
                dbQueueSize: data.data.dbQueueSize || 0,
                totalProcessed: data.data.totalProcessed,
                totalSuccess: data.data.totalSuccess,
                totalFailed: data.data.totalFailed,
              });
            } else if (data.type === 'ANTI_WEAR_STATUS' && data.data) {
              antiWearRef.current.enabled = data.data.enabled || false;
              antiWearRef.current.sorters = data.data.sorters || {};
              setAntiWearEnabled(data.data.enabled || false);
            } else if (data.barcode) {
              addPackage(data);
            }
          } catch (e) {
            console.error('解析消息失败:', e);
          }
        };

        ws.onclose = () => {
          console.log('WebSocket 已断开，5秒后重连...');
          setWsConnected(false);
          setTimeout(connectWebSocket, 5000);
        };

        ws.onerror = (error) => {
          console.error('WebSocket 错误:', error);
        };
      } catch (e) {
        console.error('创建 WebSocket 失败:', e);
        setTimeout(connectWebSocket, 5000);
      }
    };

    connectWebSocket();

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [addPackage, fetchAntiWearStatus]);

  const simulateScan = () => {
    const prefixes = ['BJ', 'SH', 'GZ', 'SZ', 'CD', 'WH', 'XA'];
    const randomPrefix = prefixes[Math.floor(Math.random() * prefixes.length)];
    const barcode = randomPrefix + Math.floor(Math.random() * 100000).toString().padStart(5, '0');

    fetch('/api/sorting/scan', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        barcode,
        scannerId: 'SCANNER-01',
        conveyorLine: 1,
        timestamp: Date.now(),
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        console.log('模拟扫描结果:', data);
      })
      .catch((err) => {
        console.error('模拟扫描失败:', err);
      });
  };

  const simulateBatch = () => {
    for (let i = 0; i < 10; i++) {
      setTimeout(() => simulateScan(), i * 300);
    }
  };

  const stressTest = (count) => {
    const prefixes = ['BJ', 'SH', 'GZ', 'SZ', 'CD', 'WH', 'XA'];
    const batch = [];
    for (let i = 0; i < count; i++) {
      const randomPrefix = prefixes[Math.floor(Math.random() * prefixes.length)];
      const barcode = randomPrefix + Math.floor(Math.random() * 100000).toString().padStart(5, '0');
      batch.push({
        barcode,
        scannerId: 'STRESS-TEST',
        conveyorLine: Math.random() > 0.5 ? 1 : 2,
        timestamp: Date.now(),
      });
    }

    fetch('/api/sorting/scan/batch', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(batch),
    })
      .then((res) => res.json())
      .then((data) => {
        console.log('压力测试结果:', data);
      })
      .catch((err) => {
        console.error('压力测试失败:', err);
        for (let i = 0; i < count; i++) {
          setTimeout(() => simulateScan(), i * 10);
        }
      });
  };

  const toggleAntiWear = () => {
    const newState = !antiWearEnabled;
    const url = newState ? '/api/antiwear/enable' : '/api/antiwear/disable';
    fetch(url, { method: 'POST' })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          antiWearRef.current.enabled = data.data?.enabled ?? newState;
          antiWearRef.current.sorters = data.data?.sorters || {};
          setAntiWearEnabled(data.data?.enabled ?? newState);
        }
      })
      .catch((err) => {
        console.error('切换减磨模式失败:', err);
      });
  };

  const stressTestSameDirection = (count, direction) => {
    const prefix = direction || 'BJ';
    for (let i = 0; i < count; i++) {
      setTimeout(() => {
        const barcode = prefix + Math.floor(Math.random() * 100000).toString().padStart(5, '0');
        fetch('/api/sorting/scan', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            barcode,
            scannerId: 'SCANNER-01',
            conveyorLine: 1,
            timestamp: Date.now(),
          }),
        });
      }, i * 250);
    }
  };

  return (
    <div className="dashboard-container">
      <div className="canvas-wrapper">
        <canvas
          ref={canvasRef}
          width={1200}
          height={580}
          className="sorting-canvas"
        />
      </div>

      <div className="control-panel">
        <h3>控制台</h3>
        <div className="control-buttons">
          <button className="btn btn-primary" onClick={simulateScan}>
            模拟扫描
          </button>
          <button className="btn btn-secondary" onClick={simulateBatch}>
            批量扫描(10个)
          </button>
        </div>

        <div className="info-section">
          <h4>分拣线概览</h4>
          <div className="info-grid">
            <div className="info-item">
              <span className="label">1号线分拣口</span>
              <span className="value">4 个</span>
            </div>
            <div className="info-item">
              <span className="label">2号线分拣口</span>
              <span className="value">3 个</span>
            </div>
            <div className="info-item">
              <span className="label">总分拣口</span>
              <span className="value">7 个</span>
            </div>
            <div className="info-item">
              <span className="label">传送速度</span>
              <span className="value">2.5 m/s</span>
            </div>
          </div>
        </div>

        <div className="legend-section">
          <h4>仓库图例</h4>
          <div className="legend-list">
            {[
              { name: '北京仓', color: '#ef4444' },
              { name: '上海仓', color: '#3b82f6' },
              { name: '广州仓', color: '#22c55e' },
              { name: '深圳仓', color: '#f59e0b' },
              { name: '成都仓', color: '#8b5cf6' },
              { name: '武汉仓', color: '#ec4899' },
              { name: '西安仓', color: '#14b8a6' },
            ].map((item) => (
              <div key={item.name} className="legend-item">
                <span
                  className="legend-color"
                  style={{ backgroundColor: item.color }}
                />
                <span>{item.name}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="monitor-section">
          <h4>实时监控</h4>
          <div className="monitor-status">
            <span className={`status-indicator status-${monitorData.systemStatus.toLowerCase()}`}>
              ● {monitorData.systemStatus === 'NORMAL' ? '正常' : monitorData.systemStatus === 'WARN' ? '警告' : '告警'}
            </span>
          </div>
          <div className="info-grid">
            <div className="info-item">
              <span className="label">当前 QPS</span>
              <span className="value">{monitorData.currentQps || 0}</span>
            </div>
            <div className="info-item">
              <span className="label">指令队列</span>
              <span className={`value ${(monitorData.queueSize || 0) > 20 ? 'text-danger' : ''}`}>
                {monitorData.queueSize || 0}
              </span>
            </div>
            <div className="info-item">
              <span className="label">平均延迟</span>
              <span className={`value ${(monitorData.avgLatency || 0) > 200 ? 'text-danger' : ''}`}>
                {monitorData.avgLatency || 0}ms
              </span>
            </div>
            <div className="info-item">
              <span className="label">最大延迟</span>
              <span className={`value ${(monitorData.maxLatency || 0) > 500 ? 'text-danger' : ''}`}>
                {monitorData.maxLatency || 0}ms
              </span>
            </div>
          </div>
          <div className="info-item">
            <span className="label">总计处理</span>
            <span className="value">{monitorData.totalProcessed || 0}</span>
          </div>
          <div className="info-item">
            <span className="label">成功/失败</span>
            <span className="value">
              <span className="text-success">{monitorData.totalSuccess || 0}</span>
              {' / '}
              <span className="text-danger">{monitorData.totalFailed || 0}</span>
            </span>
          </div>
        </div>

        <div className="antiwear-section">
          <h4>减磨模式</h4>
          <div className="antiwear-status">
            <span className={`status-indicator ${antiWearEnabled ? 'status-antiwear-on' : 'status-antiwear-off'}`}>
              ● {antiWearEnabled ? '已开启' : '已关闭'}
            </span>
          </div>
          <div className="control-buttons">
            <button
              className={`btn ${antiWearEnabled ? 'btn-danger' : 'btn-success'}`}
              onClick={toggleAntiWear}
            >
              {antiWearEnabled ? '关闭减磨模式' : '开启减磨模式'}
            </button>
          </div>
          {antiWearEnabled && (
            <p className="hint-text">
              连续5个同方向包裹后，挡板保持开启
            </p>
          )}
        </div>

        <div className="stress-test-section">
          <h4>压力测试</h4>
          <div className="control-buttons">
            <button className="btn btn-warning" onClick={() => stressTest(50)}>
              压力测试 (50)
            </button>
            <button className="btn btn-danger" onClick={() => stressTest(200)}>
              压力测试 (200)
            </button>
          </div>
          <p className="hint-text">注意：高压测试会产生大量包裹动画</p>

          <div style={{ marginTop: '10px' }}>
            <h5 style={{ color: '#94a3b8', fontSize: '12px', marginBottom: '8px' }}>同方向测试（减磨）</h5>
            <div className="control-buttons">
              <button className="btn btn-primary" onClick={() => stressTestSameDirection(10, 'BJ')} style={{ backgroundColor: '#ef4444', borderColor: '#ef4444' }}>
                北京仓×10
              </button>
              <button className="btn btn-primary" onClick={() => stressTestSameDirection(20, 'BJ')} style={{ backgroundColor: '#ef4444', borderColor: '#ef4444' }}>
                北京仓×20
              </button>
            </div>
          </div>
        </div>

        <div className="system-info">
          <h4>系统信息</h4>
          <div className="info-item">
            <span className="label">后端地址</span>
            <span className="value">localhost:8080</span>
          </div>
          <div className="info-item">
            <span className="label">连接状态</span>
            <span className={`value ${wsConnected ? 'text-success' : 'text-danger'}`}>
              {wsConnected ? 'WebSocket 已连接' : 'WebSocket 未连接'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SortingDashboard;
