export const SUPPORTED_LANGUAGES = ['zh-Hans', 'zh-Hant', 'en', 'pt']

const messages = {
  'zh-Hans': {
    affectedStops: '受影响节点', localRerouteTag: '尽量少改', localRerouteDescription: '仅处理 {count} 个受影响节点，其余行程顺序保持不变。', keepUnaffectedStops: '锁定未受影响地点', replaceAffectedStops: '替换冲突地点并重算相邻路段', useLocalReroute: '执行局部改线', globalRerouteTag: '完整重算', globalRerouteDescription: '重新求解整条路线，适合多个地点同时冲突或影响范围较大。', recalculateAllStops: '重新排列全部可用地点', recheckConstraints: '重新校验时间、步行和必去约束', useGlobalReroute: '执行整程重排', localRerouteResult: '局部改线结果', globalRerouteResult: '整程重排结果',
    currentLocation: '当前所在地', locationNotSet: '尚未设置位置', locationPrivacy: '仅在点击定位后读取，并随当前会话过期', useCurrentLocation: '使用当前位置', locating: '正在定位', manualLocation: '或手动选择澳门起点', chooseStartPoi: '选择附近的澳门地点', clearLocation: '清除位置', locationCleared: '已清除当前位置', currentPosition: '我的当前位置', manualLocationSet: '已将 {name} 设为起点', geolocationUnsupported: '当前浏览器不支持定位，请手动选择起点', outsideMacao: '当前位置不在澳门范围内，请手动选择澳门起点', locationDenied: '定位权限未开启，请允许定位或手动选择起点', locationFailed: '定位失败，请稍后重试或手动选择起点', locationReady: '定位成功，生成路线时会优先匹配附近景点',
    brand: '澳门文旅智联', subtitle: '澳门公共文旅点位目录', languageSelector: '界面语言',
    guestAction: '临时访客 · 登录保存', guestActionMobile: '登录保存', assistant: '智慧助手', assistantMobile: '助手',
    guestTitle: '当前偏好与位置仅保存在此浏览器会话，登录后可主动选择跨会话保存', assistantTitle: '进入智慧助手',
    discovery: '澳门发现', explore: '探索澳门', reload: '重新加载', workspace: '游客工作台视图', preferences: '偏好', catalog: '点位',
    searchPlaceholder: '搜索点位名称', clearSearch: '清空搜索', region: '区域', all: '全部', category: '分类', allCategories: '全部分类',
    loadingCatalog: '正在加载点位', catalogLoadFailed: '澳门点位目录加载失败', emptyCatalog: '没有符合条件的有效点位',
    loadingMap: '正在加载澳门地图', mapUnavailable: '地图暂时无法加载', mapConnectionError: '请检查高德地图 Key 或网络连接', mapServiceError: '高德地点服务加载失败',
    closeDetails: '关闭详情', naturalMerchant: '自然商户点位', openingHours: '开放时间', accessibility: '无障碍', dataSource: '数据来源', validUntil: '有效期', until: '至', viewSource: '查看来源机构',
    sessionOnly: '偏好仅保存在当前会话', saved: '已保存', loadingPreferences: '正在读取偏好', interests: '兴趣', required: '必填',
    travelTime: '出行时间', departure: '出发', latestEnd: '最晚结束', maxWalking: '最大步行距离', mustVisit: '必去地点', choosePoi: '选择澳门目录点位', removePoi: '移除 {name}',
    transportPreference: '交通偏好', language: '语言', accessibilityNeeds: '无障碍需求', reset: '重置', saving: '保存中', savePreferences: '保存偏好',
    validationInterest: '请至少选择一个兴趣', validationTime: '最晚结束时间必须晚于出发时间', validationMustVisit: '必去点最多选择 8 个',
    saveSuccess: '已保存到当前游客会话', saveFailed: '保存失败', resetSuccess: '已恢复默认值', resetFailed: '重置失败', preferenceLoadFailed: '偏好读取失败',
    meter: '米', kilometer: '公里', sourceOrganization: '澳门特别行政区政府旅游局',
    region_PENINSULA: '澳门半岛', region_TAIPA: '氹仔', region_COTAI: '路氹', region_COLOANE: '路环',
    category_ATTRACTION: '景点', category_CULTURE: '文化', category_NATURE: '自然', category_TRANSPORT: '交通', category_PUBLIC_SERVICE: '公共服务', category_FOOD: '餐饮', category_RETAIL: '社区零售', category_WELLNESS: '康养',
    accessibility_FULL: '通行条件良好', accessibility_PARTIAL: '部分区域可通行', accessibility_LIMITED: '通行条件有限',
    interest_ATTRACTION: '地标', interest_CULTURE: '文化', interest_FOOD: '美食', interest_NATURE: '自然', interest_RETAIL: '社区', interest_PUBLIC_SERVICE: '公共服务',
    transport_WALK: '步行', transport_PUBLIC_TRANSIT: '公交', transport_MIXED: '组合',
    need_STEP_FREE: '无台阶通行', need_LOW_WALKING: '低步行强度', need_QUIET_ROUTE: '安静少拥挤',
    itinerary: '行程', generateTrip: '按偏好时间生成路线', generatingTrip: '正在规划', noTrip: '保存偏好后，系统会按照出发和结束时间生成路线', resetTrip: '清空当前路线', demoRoutes: '固定演示路线', demoRoutesSaved: '会话内保存', demoGateLoop: '关闸出发 · 一日往返', demoHotelLoop: '威尼斯人酒店 · 一日往返',
    routeFeasible: '硬约束全部通过', routeConflict: '路线存在冲突', totalTime: '总时长', walkDistance: '步行', finishAt: '结束', safetyBuffer: '安全缓冲',
    mustVisitBadge: '必去', stayMinutes: '停留 {minutes} 分钟', bufferMinutes: '缓冲 {minutes} 分钟', sourceTrace: '官方来源',
    transportCompare: '交通方案对比', effort: '体力', cost: '费用', feasible: '可行', infeasible: '冲突', smartTransitRoute: '智能公交路线', boardAt: '上车站', alightAt: '下车站', viaStops: '途经站', stationMarkedOnMap: '地图已标记', unknownStationLocation: '站点坐标未返回',
    recommendations: '沿途自然推荐', addToTrip: '加入路线', remindLater: '稍后提醒', ignore: '忽略', detour: '绕行 +{minutes} 分钟 / {meters} 米',
    eventAlert: '行程受突发事件影响', affectedLocation: '影响地点', locateEvent: '在地图定位', localReroute: '局部改线', globalReroute: '整程重排', highRiskNote: '高危事件不会静默改线，请先确认安全提示。',
    undo: '撤回改线', restoreOriginal: '恢复初始路线', routeComparison: '路线变化', durationDelta: '时间变化', walkingDelta: '步行变化',
    textRoute: '文字路线', exportTxt: '导出 TXT', feedback: '提交反馈', feedbackCategory: '问题类型', feedbackContent: '问题描述', submit: '提交', close: '关闭',
    feedback_DATA_ERROR: '数据错误', feedback_ROUTE_ISSUE: '路线不合理', feedback_ACCESSIBILITY: '无障碍问题', feedback_HELP: '人工求助', feedbackSuccess: '反馈已提交',
    mapFallback: '地图服务异常，当前显示静态文字路线', officialHelp: '澳门旅游热线 +853 2833 3000', operationFailed: '操作失败', version: '版本 {version}',
    accessibilityTools: '无障碍显示', largeText: '大字体', highContrast: '高对比', simplifiedView: '简化页面'
  },
  'zh-Hant': {
    affectedStops: '受影響節點', localRerouteTag: '儘量少改', localRerouteDescription: '只處理 {count} 個受影響節點，其餘行程順序保持不變。', keepUnaffectedStops: '鎖定未受影響地點', replaceAffectedStops: '替換衝突地點並重算相鄰路段', useLocalReroute: '執行局部改線', globalRerouteTag: '完整重算', globalRerouteDescription: '重新求解整條路線，適合多個地點同時衝突或影響範圍較大。', recalculateAllStops: '重新排列全部可用地點', recheckConstraints: '重新校驗時間、步行和必去約束', useGlobalReroute: '執行整程重排', localRerouteResult: '局部改線結果', globalRerouteResult: '整程重排結果',
    currentLocation: '目前所在地', locationNotSet: '尚未設定位置', locationPrivacy: '只在點擊定位後讀取，並隨目前工作階段到期', useCurrentLocation: '使用目前位置', locating: '正在定位', manualLocation: '或手動選擇澳門起點', chooseStartPoi: '選擇附近的澳門地點', clearLocation: '清除位置', locationCleared: '已清除目前位置', currentPosition: '我的目前位置', manualLocationSet: '已將 {name} 設為起點', geolocationUnsupported: '目前瀏覽器不支援定位，請手動選擇起點', outsideMacao: '目前位置不在澳門範圍內，請手動選擇澳門起點', locationDenied: '定位權限未開啟，請允許定位或手動選擇起點', locationFailed: '定位失敗，請稍後重試或手動選擇起點', locationReady: '定位成功，產生路線時會優先配對附近景點',
    brand: '澳門文旅智聯', subtitle: '澳門公共文旅點位目錄', languageSelector: '介面語言',
    guestAction: '臨時訪客 · 登入儲存', guestActionMobile: '登入儲存', assistant: '智慧助手', assistantMobile: '助手',
    guestTitle: '目前偏好與位置僅保存在此瀏覽器工作階段，登入後可主動選擇跨工作階段儲存', assistantTitle: '進入智慧助手',
    discovery: '澳門發現', explore: '探索澳門', reload: '重新載入', workspace: '遊客工作台檢視', preferences: '偏好', catalog: '點位',
    searchPlaceholder: '搜尋點位名稱', clearSearch: '清除搜尋', region: '區域', all: '全部', category: '分類', allCategories: '全部分類',
    loadingCatalog: '正在載入點位', catalogLoadFailed: '澳門點位目錄載入失敗', emptyCatalog: '沒有符合條件的有效點位',
    loadingMap: '正在載入澳門地圖', mapUnavailable: '地圖暫時無法載入', mapConnectionError: '請檢查高德地圖 Key 或網路連線', mapServiceError: '高德地點服務載入失敗',
    closeDetails: '關閉詳情', naturalMerchant: '自然商戶點位', openingHours: '開放時間', accessibility: '無障礙', dataSource: '資料來源', validUntil: '有效期', until: '至', viewSource: '查看來源機構',
    sessionOnly: '偏好僅保存在目前工作階段', saved: '已儲存', loadingPreferences: '正在讀取偏好', interests: '興趣', required: '必填',
    travelTime: '出行時間', departure: '出發', latestEnd: '最晚結束', maxWalking: '最大步行距離', mustVisit: '必去地點', choosePoi: '選擇澳門目錄點位', removePoi: '移除 {name}',
    transportPreference: '交通偏好', language: '語言', accessibilityNeeds: '無障礙需求', reset: '重設', saving: '儲存中', savePreferences: '儲存偏好',
    validationInterest: '請至少選擇一個興趣', validationTime: '最晚結束時間必須晚於出發時間', validationMustVisit: '必去點最多選擇 8 個',
    saveSuccess: '已儲存到目前遊客工作階段', saveFailed: '儲存失敗', resetSuccess: '已恢復預設值', resetFailed: '重設失敗', preferenceLoadFailed: '偏好讀取失敗',
    meter: '米', kilometer: '公里', sourceOrganization: '澳門特別行政區政府旅遊局',
    region_PENINSULA: '澳門半島', region_TAIPA: '氹仔', region_COTAI: '路氹', region_COLOANE: '路環',
    category_ATTRACTION: '景點', category_CULTURE: '文化', category_NATURE: '自然', category_TRANSPORT: '交通', category_PUBLIC_SERVICE: '公共服務', category_FOOD: '餐飲', category_RETAIL: '社區零售', category_WELLNESS: '康養',
    accessibility_FULL: '通行條件良好', accessibility_PARTIAL: '部分區域可通行', accessibility_LIMITED: '通行條件有限',
    interest_ATTRACTION: '地標', interest_CULTURE: '文化', interest_FOOD: '美食', interest_NATURE: '自然', interest_RETAIL: '社區', interest_PUBLIC_SERVICE: '公共服務',
    transport_WALK: '步行', transport_PUBLIC_TRANSIT: '公車', transport_MIXED: '組合',
    need_STEP_FREE: '無台階通行', need_LOW_WALKING: '低步行強度', need_QUIET_ROUTE: '安靜少擁擠',
    itinerary: '行程', generateTrip: '按偏好時間產生路線', generatingTrip: '正在規劃', noTrip: '儲存偏好後，系統會按照出發和結束時間產生路線', resetTrip: '清空目前路線', demoRoutes: '固定演示路線', demoRoutesSaved: '工作階段保存', demoGateLoop: '關閘出發 · 一日往返', demoHotelLoop: '威尼斯人酒店 · 一日往返', routeFeasible: '硬約束全部通過', routeConflict: '路線存在衝突',
    totalTime: '總時長', walkDistance: '步行', finishAt: '結束', safetyBuffer: '安全緩衝', mustVisitBadge: '必去', stayMinutes: '停留 {minutes} 分鐘', bufferMinutes: '緩衝 {minutes} 分鐘', sourceTrace: '官方來源',
    transportCompare: '交通方案比較', effort: '體力', cost: '費用', feasible: '可行', infeasible: '衝突', smartTransitRoute: '智能公交路線', boardAt: '上車站', alightAt: '下車站', viaStops: '途經站', stationMarkedOnMap: '地圖已標記', unknownStationLocation: '未傳回站點座標', recommendations: '沿途自然推薦', addToTrip: '加入路線', remindLater: '稍後提醒', ignore: '忽略', detour: '繞行 +{minutes} 分鐘 / {meters} 米',
    eventAlert: '行程受突發事件影響', affectedLocation: '影響地點', locateEvent: '在地圖定位', localReroute: '局部改線', globalReroute: '整程重排', highRiskNote: '高危事件不會靜默改線，請先確認安全提示。', undo: '撤回改線', restoreOriginal: '恢復初始路線', routeComparison: '路線變化', durationDelta: '時間變化', walkingDelta: '步行變化',
    textRoute: '文字路線', exportTxt: '匯出 TXT', feedback: '提交回饋', feedbackCategory: '問題類型', feedbackContent: '問題描述', submit: '提交', close: '關閉', feedback_DATA_ERROR: '資料錯誤', feedback_ROUTE_ISSUE: '路線不合理', feedback_ACCESSIBILITY: '無障礙問題', feedback_HELP: '人工求助', feedbackSuccess: '回饋已提交',
    mapFallback: '地圖服務異常，目前顯示靜態文字路線', officialHelp: '澳門旅遊熱線 +853 2833 3000', operationFailed: '操作失敗', version: '版本 {version}', accessibilityTools: '無障礙顯示', largeText: '大字體', highContrast: '高對比', simplifiedView: '簡化頁面'
  },
  en: {
    affectedStops: 'Affected stops', localRerouteTag: 'Minimal changes', localRerouteDescription: 'Resolve only {count} affected stops and preserve the order of the rest.', keepUnaffectedStops: 'Lock unaffected stops', replaceAffectedStops: 'Replace conflicts and recalculate adjacent legs', useLocalReroute: 'Apply local reroute', globalRerouteTag: 'Full recalculation', globalRerouteDescription: 'Solve the entire route again when several places conflict or the impact is broad.', recalculateAllStops: 'Reorder every available stop', recheckConstraints: 'Recheck time, walking and must-visit constraints', useGlobalReroute: 'Replan entire route', localRerouteResult: 'Local reroute result', globalRerouteResult: 'Full replan result',
    currentLocation: 'Current location', locationNotSet: 'Location not set', locationPrivacy: 'Read only after you request it and expires with this session', useCurrentLocation: 'Use current location', locating: 'Locating', manualLocation: 'Or choose a Macao starting point', chooseStartPoi: 'Choose a nearby Macao place', clearLocation: 'Clear location', locationCleared: 'Current location cleared', currentPosition: 'My current location', manualLocationSet: '{name} is now the starting point', geolocationUnsupported: 'This browser does not support location. Choose a starting point manually.', outsideMacao: 'Your current location is outside Macao. Choose a Macao starting point manually.', locationDenied: 'Location permission was not granted. Allow it or choose a starting point manually.', locationFailed: 'Location failed. Try again or choose a starting point manually.', locationReady: 'Location found. Nearby attractions will be prioritized when generating a route.',
    brand: 'Macao Tourism Connect', subtitle: 'Macao public tourism directory', languageSelector: 'Interface language',
    guestAction: 'Guest · Sign in to save', guestActionMobile: 'Sign in', assistant: 'Smart assistant', assistantMobile: 'Assistant',
    guestTitle: 'Preferences and location stay in this browser session. Sign in to choose cross-session storage.', assistantTitle: 'Open the smart assistant',
    discovery: 'MACAO DISCOVERY', explore: 'Explore Macao', reload: 'Reload', workspace: 'Visitor workspace view', preferences: 'Preferences', catalog: 'Places',
    searchPlaceholder: 'Search place names', clearSearch: 'Clear search', region: 'Region', all: 'All', category: 'Category', allCategories: 'All categories',
    loadingCatalog: 'Loading places', catalogLoadFailed: 'Could not load the Macao directory', emptyCatalog: 'No valid places match these filters',
    loadingMap: 'Loading the Macao map', mapUnavailable: 'Map is temporarily unavailable', mapConnectionError: 'Check the AMap key or network connection', mapServiceError: 'AMap place service failed to load',
    closeDetails: 'Close details', naturalMerchant: 'Local business place', openingHours: 'Opening hours', accessibility: 'Accessibility', dataSource: 'Data source', validUntil: 'Valid until', until: 'Until', viewSource: 'View source',
    sessionOnly: 'Preferences are saved only in this session', saved: 'Saved', loadingPreferences: 'Loading preferences', interests: 'Interests', required: 'Required',
    travelTime: 'Travel time', departure: 'Departure', latestEnd: 'Latest end', maxWalking: 'Maximum walking distance', mustVisit: 'Must-visit places', choosePoi: 'Choose a place from the Macao directory', removePoi: 'Remove {name}',
    transportPreference: 'Transport preference', language: 'Language', accessibilityNeeds: 'Accessibility needs', reset: 'Reset', saving: 'Saving', savePreferences: 'Save preferences',
    validationInterest: 'Choose at least one interest', validationTime: 'The latest end must be after departure', validationMustVisit: 'Choose no more than 8 must-visit places',
    saveSuccess: 'Saved to the current visitor session', saveFailed: 'Could not save preferences', resetSuccess: 'Defaults restored', resetFailed: 'Could not reset preferences', preferenceLoadFailed: 'Could not load preferences',
    meter: 'm', kilometer: 'km', sourceOrganization: 'Macao Government Tourism Office',
    region_PENINSULA: 'Macao Peninsula', region_TAIPA: 'Taipa', region_COTAI: 'Cotai', region_COLOANE: 'Coloane',
    category_ATTRACTION: 'Attractions', category_CULTURE: 'Culture', category_NATURE: 'Nature', category_TRANSPORT: 'Transport', category_PUBLIC_SERVICE: 'Public services', category_FOOD: 'Food', category_RETAIL: 'Local retail', category_WELLNESS: 'Wellness',
    accessibility_FULL: 'Good access', accessibility_PARTIAL: 'Partial access', accessibility_LIMITED: 'Limited access',
    interest_ATTRACTION: 'Landmarks', interest_CULTURE: 'Culture', interest_FOOD: 'Food', interest_NATURE: 'Nature', interest_RETAIL: 'Local life', interest_PUBLIC_SERVICE: 'Public services',
    transport_WALK: 'Walk', transport_PUBLIC_TRANSIT: 'Transit', transport_MIXED: 'Mixed',
    need_STEP_FREE: 'Step-free access', need_LOW_WALKING: 'Low walking', need_QUIET_ROUTE: 'Quiet route',
    itinerary: 'Trip', generateTrip: 'Generate route for my time window', generatingTrip: 'Planning', noTrip: 'Save preferences to generate a route for your selected start and end time', resetTrip: 'Clear current route', demoRoutes: 'Saved demo routes', demoRoutesSaved: 'Saved in session', demoGateLoop: 'Border Gate · full-day loop', demoHotelLoop: 'Venetian hotel · full-day loop', routeFeasible: 'All hard constraints pass', routeConflict: 'Route conflicts found',
    totalTime: 'Duration', walkDistance: 'Walking', finishAt: 'Finish', safetyBuffer: 'Safety buffer', mustVisitBadge: 'Must visit', stayMinutes: 'Stay {minutes} min', bufferMinutes: 'Buffer {minutes} min', sourceTrace: 'Official source',
    transportCompare: 'Transport comparison', effort: 'Effort', cost: 'Cost', feasible: 'Feasible', infeasible: 'Conflict', smartTransitRoute: 'Smart transit route', boardAt: 'Board at', alightAt: 'Alight at', viaStops: 'Via stops', stationMarkedOnMap: 'Marked on map', unknownStationLocation: 'Station coordinates unavailable', recommendations: 'On-route public recommendations', addToTrip: 'Add to route', remindLater: 'Remind later', ignore: 'Ignore', detour: 'Detour +{minutes} min / {meters} m',
    eventAlert: 'An event affects this trip', affectedLocation: 'Affected location', locateEvent: 'Locate on map', localReroute: 'Local reroute', globalReroute: 'Replan all', highRiskNote: 'High-risk events never reroute silently. Review the safety notice first.', undo: 'Undo reroute', restoreOriginal: 'Restore original', routeComparison: 'Route changes', durationDelta: 'Time change', walkingDelta: 'Walking change',
    textRoute: 'Text route', exportTxt: 'Export TXT', feedback: 'Send feedback', feedbackCategory: 'Issue type', feedbackContent: 'Describe the issue', submit: 'Submit', close: 'Close', feedback_DATA_ERROR: 'Data error', feedback_ROUTE_ISSUE: 'Route issue', feedback_ACCESSIBILITY: 'Accessibility', feedback_HELP: 'Human help', feedbackSuccess: 'Feedback submitted',
    mapFallback: 'Map service unavailable. Showing the static text route.', officialHelp: 'Macao tourism hotline +853 2833 3000', operationFailed: 'Operation failed', version: 'Version {version}', accessibilityTools: 'Accessibility display', largeText: 'Large text', highContrast: 'High contrast', simplifiedView: 'Simplified view'
  },
  pt: {
    affectedStops: 'Paragens afetadas', localRerouteTag: 'Alteracoes minimas', localRerouteDescription: 'Resolve apenas {count} paragens afetadas e mantem a ordem das restantes.', keepUnaffectedStops: 'Bloquear paragens nao afetadas', replaceAffectedStops: 'Substituir conflitos e recalcular trocos adjacentes', useLocalReroute: 'Aplicar desvio local', globalRerouteTag: 'Recalculo completo', globalRerouteDescription: 'Volta a calcular toda a rota quando varios locais entram em conflito ou o impacto e amplo.', recalculateAllStops: 'Reordenar todos os locais disponiveis', recheckConstraints: 'Rever tempo, caminhada e locais obrigatorios', useGlobalReroute: 'Replanear toda a rota', localRerouteResult: 'Resultado do desvio local', globalRerouteResult: 'Resultado do novo plano',
    currentLocation: 'Localizacao atual', locationNotSet: 'Localizacao nao definida', locationPrivacy: 'Lida apenas apos o seu pedido e eliminada no fim desta sessao', useCurrentLocation: 'Usar localizacao atual', locating: 'A localizar', manualLocation: 'Ou escolha um ponto de partida em Macau', chooseStartPoi: 'Escolha um local proximo em Macau', clearLocation: 'Limpar localizacao', locationCleared: 'Localizacao atual removida', currentPosition: 'A minha localizacao atual', manualLocationSet: '{name} e agora o ponto de partida', geolocationUnsupported: 'Este navegador nao suporta localizacao. Escolha manualmente um ponto de partida.', outsideMacao: 'A sua localizacao esta fora de Macau. Escolha manualmente um ponto de partida em Macau.', locationDenied: 'A permissao de localizacao nao foi concedida. Autorize-a ou escolha um ponto manualmente.', locationFailed: 'Falha ao obter a localizacao. Tente novamente ou escolha um ponto manualmente.', locationReady: 'Localizacao obtida. As atracoes proximas terao prioridade ao criar a rota.',
    brand: 'Macao Turismo Conectado', subtitle: 'Diretorio publico de turismo de Macau', languageSelector: 'Idioma da interface',
    guestAction: 'Visitante · Entrar para guardar', guestActionMobile: 'Entrar', assistant: 'Assistente inteligente', assistantMobile: 'Assistente',
    guestTitle: 'As preferencias e a localizacao ficam apenas nesta sessao. Entre para optar por guardar entre sessoes.', assistantTitle: 'Abrir o assistente inteligente',
    discovery: 'DESCOBRIR MACAU', explore: 'Explorar Macau', reload: 'Recarregar', workspace: 'Vista da area do visitante', preferences: 'Preferencias', catalog: 'Locais',
    searchPlaceholder: 'Pesquisar locais', clearSearch: 'Limpar pesquisa', region: 'Zona', all: 'Todas', category: 'Categoria', allCategories: 'Todas as categorias',
    loadingCatalog: 'A carregar locais', catalogLoadFailed: 'Nao foi possivel carregar o diretorio de Macau', emptyCatalog: 'Nenhum local valido corresponde aos filtros',
    loadingMap: 'A carregar o mapa de Macau', mapUnavailable: 'O mapa esta temporariamente indisponivel', mapConnectionError: 'Verifique a chave do AMap ou a ligacao de rede', mapServiceError: 'Falha ao carregar o servico de locais do AMap',
    closeDetails: 'Fechar detalhes', naturalMerchant: 'Comercio local', openingHours: 'Horario', accessibility: 'Acessibilidade', dataSource: 'Fonte de dados', validUntil: 'Valido ate', until: 'Ate', viewSource: 'Ver fonte',
    sessionOnly: 'As preferencias ficam apenas nesta sessao', saved: 'Guardado', loadingPreferences: 'A carregar preferencias', interests: 'Interesses', required: 'Obrigatorio',
    travelTime: 'Horario da viagem', departure: 'Partida', latestEnd: 'Fim mais tardio', maxWalking: 'Distancia maxima a pe', mustVisit: 'Locais obrigatorios', choosePoi: 'Escolha um local do diretorio de Macau', removePoi: 'Remover {name}',
    transportPreference: 'Preferencia de transporte', language: 'Idioma', accessibilityNeeds: 'Necessidades de acessibilidade', reset: 'Repor', saving: 'A guardar', savePreferences: 'Guardar preferencias',
    validationInterest: 'Escolha pelo menos um interesse', validationTime: 'O fim deve ser posterior a partida', validationMustVisit: 'Escolha no maximo 8 locais obrigatorios',
    saveSuccess: 'Guardado na sessao atual do visitante', saveFailed: 'Nao foi possivel guardar', resetSuccess: 'Valores predefinidos repostos', resetFailed: 'Nao foi possivel repor', preferenceLoadFailed: 'Nao foi possivel carregar as preferencias',
    meter: 'm', kilometer: 'km', sourceOrganization: 'Direccao dos Servicos de Turismo do Governo de Macau',
    region_PENINSULA: 'Peninsula de Macau', region_TAIPA: 'Taipa', region_COTAI: 'Cotai', region_COLOANE: 'Coloane',
    category_ATTRACTION: 'Atracoes', category_CULTURE: 'Cultura', category_NATURE: 'Natureza', category_TRANSPORT: 'Transportes', category_PUBLIC_SERVICE: 'Servicos publicos', category_FOOD: 'Gastronomia', category_RETAIL: 'Comercio local', category_WELLNESS: 'Bem-estar',
    accessibility_FULL: 'Bom acesso', accessibility_PARTIAL: 'Acesso parcial', accessibility_LIMITED: 'Acesso limitado',
    interest_ATTRACTION: 'Monumentos', interest_CULTURE: 'Cultura', interest_FOOD: 'Gastronomia', interest_NATURE: 'Natureza', interest_RETAIL: 'Vida local', interest_PUBLIC_SERVICE: 'Servicos publicos',
    transport_WALK: 'A pe', transport_PUBLIC_TRANSIT: 'Transportes', transport_MIXED: 'Combinado',
    need_STEP_FREE: 'Acesso sem degraus', need_LOW_WALKING: 'Pouca caminhada', need_QUIET_ROUTE: 'Percurso tranquilo',
    itinerary: 'Itinerario', generateTrip: 'Criar rota para o horario escolhido', generatingTrip: 'A planear', noTrip: 'Guarde as preferencias para criar uma rota entre a hora de inicio e de fim escolhidas', resetTrip: 'Limpar rota atual', demoRoutes: 'Rotas de demonstracao', demoRoutesSaved: 'Guardadas na sessao', demoGateLoop: 'Portas do Cerco · circuito diário', demoHotelLoop: 'Hotel Venetian · circuito diário', routeFeasible: 'Todas as restricoes cumpridas', routeConflict: 'Conflitos na rota',
    totalTime: 'Duracao', walkDistance: 'Caminhada', finishAt: 'Fim', safetyBuffer: 'Margem de seguranca', mustVisitBadge: 'Obrigatorio', stayMinutes: 'Permanencia {minutes} min', bufferMinutes: 'Margem {minutes} min', sourceTrace: 'Fonte oficial',
    transportCompare: 'Comparar transportes', effort: 'Esforco', cost: 'Custo', feasible: 'Viavel', infeasible: 'Conflito', smartTransitRoute: 'Rota inteligente de autocarro', boardAt: 'Embarcar em', alightAt: 'Sair em', viaStops: 'Paragens intermedias', stationMarkedOnMap: 'Marcado no mapa', unknownStationLocation: 'Coordenadas indisponiveis', recommendations: 'Recomendacoes publicas no percurso', addToTrip: 'Adicionar a rota', remindLater: 'Lembrar depois', ignore: 'Ignorar', detour: 'Desvio +{minutes} min / {meters} m',
    eventAlert: 'Um evento afeta esta viagem', affectedLocation: 'Local afetado', locateEvent: 'Localizar no mapa', localReroute: 'Desvio local', globalReroute: 'Replanear tudo', highRiskNote: 'Eventos de alto risco nunca alteram a rota em silencio. Consulte primeiro o aviso.', undo: 'Anular desvio', restoreOriginal: 'Repor original', routeComparison: 'Alteracoes da rota', durationDelta: 'Mudanca de tempo', walkingDelta: 'Mudanca a pe',
    textRoute: 'Rota em texto', exportTxt: 'Exportar TXT', feedback: 'Enviar comentario', feedbackCategory: 'Tipo de problema', feedbackContent: 'Descreva o problema', submit: 'Enviar', close: 'Fechar', feedback_DATA_ERROR: 'Erro de dados', feedback_ROUTE_ISSUE: 'Problema da rota', feedback_ACCESSIBILITY: 'Acessibilidade', feedback_HELP: 'Ajuda humana', feedbackSuccess: 'Comentario enviado',
    mapFallback: 'Servico de mapa indisponivel. A mostrar rota estatica em texto.', officialHelp: 'Linha de turismo de Macau +853 2833 3000', operationFailed: 'Operacao falhou', version: 'Versao {version}', accessibilityTools: 'Visualizacao acessivel', largeText: 'Texto grande', highContrast: 'Alto contraste', simplifiedView: 'Vista simplificada'
  }
}

const openingTerms = {
  'Open space': { 'zh-Hans': '开放空间', 'zh-Hant': '開放空間', en: 'Open space', pt: 'Espaco aberto' },
  'Service dependent': { 'zh-Hans': '以服务时间为准', 'zh-Hant': '以服務時間為準', en: 'Service dependent', pt: 'Conforme o horario do servico' },
  'Event dependent': { 'zh-Hans': '以活动时间为准', 'zh-Hant': '以活動時間為準', en: 'Event dependent', pt: 'Conforme o horario do evento' },
  'Performance schedule': { 'zh-Hans': '以演出时间为准', 'zh-Hant': '以演出時間為準', en: 'Performance schedule', pt: 'Conforme o horario do espetaculo' },
  'Exhibition dependent': { 'zh-Hans': '以展览时间为准', 'zh-Hant': '以展覽時間為準', en: 'Exhibition dependent', pt: 'Conforme o horario da exposicao' },
  'Refer to official schedule': { 'zh-Hans': '请参阅官方时间表', 'zh-Hant': '請參閱官方時間表', en: 'Refer to official schedule', pt: 'Consulte o horario oficial' },
  '24 hours': { 'zh-Hans': '24 小时', 'zh-Hant': '24 小時', en: '24 hours', pt: '24 horas' }
}

const closedDays = {
  Monday: { 'zh-Hans': '周一闭馆', 'zh-Hant': '週一閉館', en: 'Monday closed', pt: 'Encerrado a segunda-feira' },
  Tuesday: { 'zh-Hans': '周二闭馆', 'zh-Hant': '週二閉館', en: 'Tuesday closed', pt: 'Encerrado a terca-feira' },
  Wednesday: { 'zh-Hans': '周三闭馆', 'zh-Hant': '週三閉館', en: 'Wednesday closed', pt: 'Encerrado a quarta-feira' },
  Thursday: { 'zh-Hans': '周四闭馆', 'zh-Hant': '週四閉館', en: 'Thursday closed', pt: 'Encerrado a quinta-feira' }
}

export function normalizeLanguage(language) {
  return SUPPORTED_LANGUAGES.includes(language) ? language : 'zh-Hans'
}

export function translate(language, key, params = {}) {
  const normalized = normalizeLanguage(language)
  const chain = normalized === 'zh-Hans' ? ['zh-Hans'] : [normalized, 'zh-Hant', 'zh-Hans']
  let value = key
  for (const candidate of chain) {
    if (messages[candidate]?.[key]) {
      value = messages[candidate][key]
      break
    }
  }
  return Object.entries(params).reduce((text, [name, replacement]) => text.replace(`{${name}}`, replacement), value)
}

export function localizeOpeningHours(value, language) {
  const normalized = normalizeLanguage(language)
  if (!value) return ''
  if (openingTerms[value]) return openingTerms[value][normalized] || openingTerms[value]['zh-Hant'] || openingTerms[value]['zh-Hans']
  const match = value.match(/^(.+); (Monday|Tuesday|Wednesday|Thursday) closed$/)
  if (!match) return value
  return `${match[1]}; ${closedDays[match[2]][normalized] || closedDays[match[2]]['zh-Hant']}`
}
