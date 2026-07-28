<template>
  <section class="trip-shell">
    <div v-if="!trip" class="trip-empty">
      <i class="fas fa-route"></i>
      <p>{{ t('noTrip') }}</p>
      <button class="primary-command" :disabled="busy" @click="generate">
        <span v-if="busy" class="spinner light"></span>
        <i v-else class="fas fa-wand-magic-sparkles"></i>{{ busy ? t('generatingTrip') : t('generateTrip') }}
      </button>
    </div>

    <template v-else>
      <div class="trip-toolbar">
        <span>{{ t('version', { version: trip.version }) }}</span>
        <button class="icon-command" :title="t('generateTrip')" :disabled="busy" @click="generate"><i class="fas fa-rotate"></i></button>
        <button class="icon-command danger" :title="t('resetTrip')" :disabled="busy" @click="resetTrip"><i class="fas fa-trash-can"></i></button>
        <button class="icon-command" :title="t('textRoute')" @click="showText = true"><i class="fas fa-file-lines"></i></button>
        <button class="icon-command" :title="t('feedback')" @click="showFeedback = true"><i class="fas fa-comment-dots"></i></button>
      </div>

      <div class="route-state" :class="trip.feasible ? 'success' : 'conflict'">
        <i :class="trip.feasible ? 'fas fa-circle-check' : 'fas fa-triangle-exclamation'"></i>
        <strong>{{ trip.feasible ? t('routeFeasible') : t('routeConflict') }}</strong>
      </div>
      <ul v-if="trip.conflicts?.length" class="conflict-list"><li v-for="item in trip.conflicts" :key="item">{{ item }}</li></ul>
      <ul v-if="trip.warnings?.length" class="warning-list"><li v-for="item in trip.warnings" :key="item">{{ item }}</li></ul>
      <p v-if="message" class="trip-message" :class="{ error: messageError }">{{ message }}</p>
      <p v-if="trip.static_fallback" class="fallback-note"><i class="fas fa-map"></i>{{ t('mapFallback') }}</p>

      <div v-if="affectedEvents.length" class="event-impact">
        <strong><i class="fas fa-triangle-exclamation"></i>{{ t('eventAlert') }}</strong>
        <span v-for="event in affectedEvents" :key="event.event_id">{{ event.title }} · {{ event.severity }}</span>
        <p class="affected-stops"><i class="fas fa-location-dot"></i>{{ t('affectedStops') }}：{{ affectedStopNames.join('、') }}</p>
        <small v-if="affectedEvents.some(event => event.severity === 'HIGH')">{{ t('highRiskNote') }}</small>
        <div class="reroute-strategies">
          <article>
            <header><i class="fas fa-code-branch"></i><span><strong>{{ t('localReroute') }}</strong><small>{{ t('localRerouteTag') }}</small></span></header>
            <p>{{ t('localRerouteDescription', { count: affectedStopNames.length }) }}</p>
            <ul><li>{{ t('keepUnaffectedStops') }}</li><li>{{ t('replaceAffectedStops') }}</li></ul>
            <button :disabled="busy" @click="reroute('LOCAL')"><span v-if="rerouteMode === 'LOCAL'" class="spinner"></span><i v-else class="fas fa-code-branch"></i>{{ t('useLocalReroute') }}</button>
          </article>
          <article>
            <header><i class="fas fa-shuffle"></i><span><strong>{{ t('globalReroute') }}</strong><small>{{ t('globalRerouteTag') }}</small></span></header>
            <p>{{ t('globalRerouteDescription') }}</p>
            <ul><li>{{ t('recalculateAllStops') }}</li><li>{{ t('recheckConstraints') }}</li></ul>
            <button :disabled="busy" @click="reroute('GLOBAL')"><span v-if="rerouteMode === 'GLOBAL'" class="spinner"></span><i v-else class="fas fa-shuffle"></i>{{ t('useGlobalReroute') }}</button>
          </article>
        </div>
      </div>

      <div class="trip-metrics">
        <div><span>{{ t('totalTime') }}</span><strong>{{ trip.total_duration_minutes }} min</strong></div>
        <div><span>{{ t('walkDistance') }}</span><strong>{{ formatDistance(trip.total_walking_meters) }}</strong></div>
        <div><span>{{ t('finishAt') }}</span><strong>{{ trip.estimated_end_time }}</strong></div>
        <div><span>{{ t('safetyBuffer') }}</span><strong>{{ trip.safety_buffer_minutes }} min</strong></div>
      </div>

      <section v-if="trip.comparison" class="comparison-band">
        <strong>{{ t('routeComparison') }} <em v-if="rerouteResultMode">{{ rerouteResultMode }}</em></strong>
        <span>{{ t('durationDelta') }} {{ signed(trip.comparison.duration_delta_minutes) }} min</span>
        <span>{{ t('walkingDelta') }} {{ signed(trip.comparison.walking_delta_meters) }} m</span>
      </section>

      <ol class="timeline">
        <li v-for="(stop, index) in trip.stops" :key="stop.poi_code">
          <span class="stop-number">{{ index + 1 }}</span>
          <div class="stop-copy">
            <div><strong>{{ stop.name }}</strong><em v-if="stop.must_visit">{{ t('mustVisitBadge') }}</em></div>
            <p>{{ stop.arrival_time }}–{{ stop.departure_time }} · {{ t('stayMinutes', { minutes: stop.stay_minutes }) }}</p>
            <small>{{ t('bufferMinutes', { minutes: stop.safety_buffer_minutes }) }}</small>
            <a :href="stop.source_url" target="_blank" rel="noopener">{{ t('sourceTrace') }} <i class="fas fa-arrow-up-right-from-square"></i></a>
          </div>
        </li>
      </ol>

      <details class="transport-details">
        <summary>{{ t('transportCompare') }}</summary>
        <div v-for="option in trip.transport_options" :key="option.mode" class="transport-row">
          <strong>{{ t(`transport_${option.mode}`) }}</strong>
          <span>{{ option.duration_minutes }} min</span><span>{{ formatDistance(option.walking_meters) }}</span>
          <span>MOP {{ option.estimated_cost_mop }}</span>
          <i :class="option.feasible ? 'fas fa-check-circle pass' : 'fas fa-circle-xmark fail'" :title="option.feasible ? t('feasible') : t('infeasible')"></i>
        </div>
      </details>

      <section v-if="transitPlans.length" class="transit-plans">
        <h3><i class="fas fa-bus-simple"></i>{{ t('amapTransitPlan') }}</h3>
        <article v-for="plan in transitPlans" :key="plan.leg_index">
          <header>
            <strong>{{ plan.from_name }} <i class="fas fa-arrow-right"></i> {{ plan.to_name }}</strong>
            <span>{{ plan.duration_minutes }} min</span>
          </header>
          <div v-for="(ride, rideIndex) in plan.rides" :key="`${plan.leg_index}-${rideIndex}`" class="transit-ride">
            <p><i class="fas fa-bus"></i><strong>{{ ride.line_name }}</strong></p>
            <div class="station-row board">
              <i class="fas fa-circle-up"></i>
              <span><em>{{ t('boardAt') }}</em><strong>{{ ride.boarding_stop }}</strong><small>{{ coordinate(ride.boarding_location) }} · {{ t('stationMarkedOnMap') }}</small></span>
            </div>
            <div v-if="ride.via_stops?.length" class="via-stops">
              {{ t('viaStops') }}: {{ ride.via_stops.join(' · ') }}
            </div>
            <div class="station-row alight">
              <i class="fas fa-circle-down"></i>
              <span><em>{{ t('alightAt') }}</em><strong>{{ ride.alighting_stop }}</strong><small>{{ coordinate(ride.alighting_location) }} · {{ t('stationMarkedOnMap') }}</small></span>
            </div>
          </div>
        </article>
      </section>

      <section v-if="trip.recommendations?.length" class="recommendation-section">
        <h3>{{ t('recommendations') }}</h3>
        <article v-for="item in trip.recommendations" :key="item.poi_code" class="recommendation-item">
          <div><strong>{{ item.name }}</strong><span>{{ categoryLabel(item.category) }}</span></div>
          <p>{{ item.reason }}</p>
          <small>{{ t('detour', { minutes: item.detour_minutes, meters: item.detour_meters }) }}</small>
          <div class="recommendation-actions">
            <button :disabled="busy" @click="addRecommendation(item.poi_code)"><i class="fas fa-plus"></i>{{ t('addToTrip') }}</button>
            <button :class="{ active: reminders.has(item.poi_code) }" @click="toggleReminder(item.poi_code)"><i class="far fa-clock"></i>{{ t('remindLater') }}</button>
            <button class="icon-command" :title="t('ignore')" @click="ignoreRecommendation(item.poi_code)"><i class="fas fa-eye-slash"></i></button>
          </div>
        </article>
      </section>

      <div class="history-actions">
        <button :disabled="busy" @click="undo"><i class="fas fa-rotate-left"></i>{{ t('undo') }}</button>
        <button :disabled="busy" @click="restore"><i class="fas fa-clock-rotate-left"></i>{{ t('restoreOriginal') }}</button>
      </div>
    </template>

    <div v-if="showText" class="modal-backdrop" @click.self="showText = false">
      <section class="trip-modal" role="dialog" :aria-label="t('textRoute')">
        <header><h3>{{ t('textRoute') }}</h3><button class="icon-command" :title="t('close')" @click="showText = false"><i class="fas fa-xmark"></i></button></header>
        <pre>{{ textRoute }}</pre>
        <button class="primary-command" @click="exportText"><i class="fas fa-download"></i>{{ t('exportTxt') }}</button>
      </section>
    </div>

    <div v-if="showFeedback" class="modal-backdrop" @click.self="showFeedback = false">
      <form class="trip-modal" role="dialog" :aria-label="t('feedback')" @submit.prevent="sendFeedback">
        <header><h3>{{ t('feedback') }}</h3><button type="button" class="icon-command" :title="t('close')" @click="showFeedback = false"><i class="fas fa-xmark"></i></button></header>
        <label>{{ t('feedbackCategory') }}<select v-model="feedback.category"><option v-for="category in feedbackCategories" :key="category" :value="category">{{ t(`feedback_${category}`) }}</option></select></label>
        <label>{{ t('feedbackContent') }}<textarea v-model.trim="feedback.content" maxlength="1000" required></textarea></label>
        <button class="primary-command"><i class="fas fa-paper-plane"></i>{{ t('submit') }}</button>
      </form>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { addTripRecommendation, createTrip, fetchCurrentTrip, ignoreTripRecommendation, rerouteTrip, resetCurrentTrip, restoreTrip, submitFeedback, undoTrip } from '../utils/api.js'
import { translate } from '../utils/i18n.js'

const props = defineProps({
  language: { type: String, default: 'zh-Hans' },
  activeEvents: { type: Array, default: () => [] },
  transitPlans: { type: Array, default: () => [] }
})
const emit = defineEmits(['trip-change'])
const trip = ref(null)
const busy = ref(false)
const rerouteMode = ref(null)
const message = ref('')
const messageError = ref(false)
const showText = ref(false)
const showFeedback = ref(false)
const reminders = reactive(new Set())
const feedbackCategories = ['DATA_ERROR', 'ROUTE_ISSUE', 'ACCESSIBILITY', 'HELP']
const feedback = reactive({ category: 'ROUTE_ISSUE', content: '' })
const t = (key, params) => translate(props.language, key, params)
const categoryLabel = category => t(`category_${category}`)
const affectedEvents = computed(() => {
  const codes = new Set(trip.value?.stops?.map(stop => stop.poi_code) || [])
  return props.activeEvents.filter(event => event.status === 'ACTIVE' && event.affected_poi_codes?.some(code => codes.has(code)))
})
const affectedStopNames = computed(() => {
  const blocked = new Set(affectedEvents.value.flatMap(event => event.affected_poi_codes || []))
  return trip.value?.stops?.filter(stop => blocked.has(stop.poi_code)).map(stop => stop.name) || []
})
const rerouteResultMode = computed(() => trip.value?.status === 'LOCAL_REROUTE'
  ? t('localRerouteResult') : trip.value?.status === 'GLOBAL_REROUTE' ? t('globalRerouteResult') : '')
const textRoute = computed(() => {
  if (!trip.value) return ''
  const lines = [`${t('itinerary')} ${trip.value.trip_id} · ${t('version', { version: trip.value.version })}`,
    `${trip.value.departure_time} → ${trip.value.estimated_end_time} · ${formatDistance(trip.value.total_walking_meters)}`, '']
  trip.value.stops.forEach((stop, index) => {
    const leg = trip.value.legs[index]
    lines.push(`${index + 1}. ${stop.arrival_time} ${stop.name}`)
    lines.push(`   ${leg?.instruction || ''} · ${t('stayMinutes', { minutes: stop.stay_minutes })}`)
    lines.push(`   ${t('sourceTrace')}: ${stop.source_url}`)
  })
  if (trip.value.static_fallback) lines.push('', t('mapFallback'))
  lines.push('', t('officialHelp'))
  return lines.join('\n')
})

function applyTrip(value) {
  trip.value = value
  emit('trip-change', value)
}
function formatDistance(value) { return value >= 1000 ? `${(value / 1000).toFixed(1)} km` : `${value} m` }
function coordinate(value) { return Array.isArray(value) ? `${value[1].toFixed(5)}, ${value[0].toFixed(5)}` : t('unknownStationLocation') }
function signed(value) { return value > 0 ? `+${value}` : String(value) }
async function run(action) {
  busy.value = true; message.value = ''; messageError.value = false
  try { applyTrip(await action()) } catch (error) { messageError.value = true; message.value = error.message || t('operationFailed') } finally { busy.value = false }
}
function generate() { return run(() => createTrip({ required_arrival_times: {} })) }
async function resetTrip() {
  busy.value = true; message.value = ''
  try {
    await resetCurrentTrip()
    reminders.clear(); showText.value = false; showFeedback.value = false
    applyTrip(null)
  } catch (error) {
    messageError.value = true; message.value = error.message || t('operationFailed')
  } finally { busy.value = false }
}
function addRecommendation(code) { return run(() => addTripRecommendation(trip.value.trip_id, code)) }
function ignoreRecommendation(code) { return run(() => ignoreTripRecommendation(trip.value.trip_id, code)) }
async function reroute(mode) {
  rerouteMode.value = mode
  await run(() => rerouteTrip(trip.value.trip_id, mode))
  rerouteMode.value = null
}
function undo() { return run(() => undoTrip(trip.value.trip_id)) }
function restore() { return run(() => restoreTrip(trip.value.trip_id)) }
function toggleReminder(code) { reminders.has(code) ? reminders.delete(code) : reminders.add(code) }
function exportText() {
  const blob = new Blob([textRoute.value], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = `${trip.value.trip_id}.txt`; link.click(); URL.revokeObjectURL(link.href)
}
async function sendFeedback() {
  try {
    await submitFeedback({ ...feedback, trip_id: trip.value?.trip_id, trip_version: trip.value?.version, event_id: affectedEvents.value[0]?.event_id })
    feedback.content = ''; showFeedback.value = false; messageError.value = false; message.value = t('feedbackSuccess')
  } catch (error) { messageError.value = true; message.value = error.message || t('operationFailed') }
}
onMounted(async () => { try { const current = await fetchCurrentTrip(); if (current) applyTrip(current) } catch (_) {} })
</script>

<style scoped>
.trip-shell { flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; }
.trip-empty { min-height: 290px; display: grid; place-items: center; align-content: center; gap: 12px; color: #68777e; text-align: center; }
.trip-empty > i { font-size: 34px; color: #176d5d; }.trip-empty p { max-width: 240px; margin: 0; font-size: 13px; line-height: 1.5; }
.primary-command { min-height: 38px; border: 1px solid #176d5d; border-radius: 6px; padding: 0 14px; background: #176d5d; color: #fff; font-weight: 800; cursor: pointer; }.primary-command i { margin-right: 6px; }
.trip-toolbar { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; }.trip-toolbar > span { margin-right: auto; color: #65757c; font-size: 11px; font-weight: 700; }
.icon-command { width: 32px; height: 32px; border: 1px solid #dce3e4; border-radius: 5px; background: #fff; color: #52636b; cursor: pointer; }
.route-state { display: flex; align-items: center; gap: 7px; min-height: 38px; padding: 8px 10px; border-radius: 6px; font-size: 12px; }.route-state.success { background: #e9f4f1; color: #176d5d; }.route-state.conflict { background: #faece9; color: #9d3f34; }
.conflict-list { margin: 7px 0; padding-left: 20px; color: #9d3f34; font-size: 11px; }.warning-list { margin: 7px 0; padding: 7px 7px 7px 24px; border-left: 3px solid #c78922; background: #fff7df; color: #70520f; font-size: 11px; line-height: 1.5; }.fallback-note { margin: 7px 0; padding: 7px; background: #fff4d8; color: #765d18; font-size: 11px; }.fallback-note i { margin-right: 6px; }
.event-impact { display: grid; gap: 6px; margin: 8px 0; padding: 9px; border: 1px solid #e4b9ac; border-radius: 6px; background: #fff7f4; color: #773e34; font-size: 11px; }.event-impact > strong i { margin-right: 6px; }.event-impact > small { line-height: 1.45; }.affected-stops { margin: 2px 0; padding: 6px 7px; border-left: 3px solid #b64d3f; background: rgba(182,77,63,.07); color: #6f3c34; font-weight: 700; }.affected-stops i { margin-right: 5px; }.reroute-strategies { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; margin-top: 3px; }.reroute-strategies article { min-width: 0; display: flex; flex-direction: column; padding: 8px; border: 1px solid #ddb8ae; border-radius: 6px; background: #fff; }.reroute-strategies header { display: grid; grid-template-columns: 23px 1fr; gap: 5px; align-items: center; }.reroute-strategies header > i { width: 23px; height: 23px; display: grid; place-items: center; border-radius: 4px; background: #f7e5e0; color: #a3483b; }.reroute-strategies header span { min-width: 0; display: grid; gap: 1px; }.reroute-strategies header strong { color: #68372f; font-size: 10px; }.reroute-strategies header small { color: #8b6d67; font-size: 8px; }.reroute-strategies p { min-height: 36px; margin: 7px 0 4px; color: #68504b; font-size: 9px; line-height: 1.45; }.reroute-strategies ul { flex: 1; margin: 0 0 7px; padding-left: 15px; color: #75605b; font-size: 8px; line-height: 1.6; }.reroute-strategies button { min-height: 31px; border: 1px solid #b95c4b; border-radius: 5px; background: #fff; color: #8b4035; font-size: 9px; font-weight: 800; cursor: pointer; }.reroute-strategies button:disabled { opacity: .6; cursor: wait; }.reroute-strategies button i { margin-right: 5px; }.reroute-strategies .spinner { border-color: #e3bab2; border-top-color: #a3483b; }
.trip-metrics { display: grid; grid-template-columns: 1fr 1fr; border-block: 1px solid #e1e6e7; margin: 10px 0; }.trip-metrics div { display: flex; justify-content: space-between; gap: 6px; padding: 8px 4px; font-size: 11px; }.trip-metrics span { color: #748188; }.trip-metrics strong { color: #263840; }
.comparison-band { display: flex; flex-wrap: wrap; gap: 5px 10px; padding: 8px; background: #eef4f7; color: #415e6a; font-size: 11px; }.comparison-band strong { width: 100%; }.comparison-band em { margin-left: 5px; padding: 2px 4px; border-radius: 3px; background: #dce9ed; color: #315867; font-size: 8px; font-style: normal; }
.timeline { list-style: none; margin: 12px 0; padding: 0; }.timeline li { position: relative; display: grid; grid-template-columns: 28px 1fr; gap: 8px; padding-bottom: 13px; }.timeline li:not(:last-child)::after { content: ''; position: absolute; left: 13px; top: 27px; bottom: 0; width: 2px; background: #cfe0dc; }.stop-number { width: 28px; height: 28px; display: grid; place-items: center; border-radius: 50%; background: #176d5d; color: #fff; font-size: 11px; font-weight: 800; z-index: 1; }.stop-copy { min-width: 0; }.stop-copy > div { display: flex; align-items: center; gap: 6px; }.stop-copy strong { font-size: 12px; }.stop-copy em { padding: 2px 4px; border-radius: 3px; background: #f7e5d5; color: #8b4f24; font-size: 9px; font-style: normal; }.stop-copy p,.stop-copy small { margin: 3px 0; color: #69777e; font-size: 10px; }.stop-copy a { color: #176d5d; font-size: 10px; text-decoration: none; }
.transport-details { border-top: 1px solid #e1e6e7; padding-top: 10px; }.transport-details summary { color: #40545d; font-size: 12px; font-weight: 800; cursor: pointer; }.transport-row { display: grid; grid-template-columns: 1.3fr repeat(3,1fr) 18px; gap: 4px; align-items: center; padding: 7px 0; border-bottom: 1px solid #edf0f0; font-size: 9px; }.transport-row .pass { color: #176d5d; }.transport-row .fail { color: #a54b3e; }
.transit-plans { margin-top: 13px; border-top: 1px solid #e1e6e7; padding-top: 10px; }.transit-plans > h3 { display: flex; align-items: center; gap: 6px; margin: 0 0 8px; color: #355f76; font-size: 12px; }.transit-plans > article { margin-bottom: 10px; padding: 9px; border: 1px solid #d9e4e8; border-radius: 6px; background: #f8fbfc; }.transit-plans article > header { display: flex; justify-content: space-between; gap: 8px; font-size: 10px; }.transit-plans article > header span { color: #617780; }.transit-ride { margin-top: 8px; padding-top: 7px; border-top: 1px solid #e1eaed; }.transit-ride > p { display: flex; gap: 6px; margin: 0 0 7px; color: #295f7b; font-size: 10px; }.station-row { display: grid; grid-template-columns: 17px 1fr; gap: 5px; color: #50646e; }.station-row > i { margin-top: 3px; font-size: 10px; }.station-row.board > i { color: #168061; }.station-row.alight > i { color: #b75b43; }.station-row span { display: grid; gap: 1px; }.station-row em { color: #7b8a90; font-size: 8px; font-style: normal; }.station-row strong { font-size: 10px; }.station-row small { color: #75868d; font-size: 8px; }.via-stops { margin: 5px 0 5px 22px; color: #72838a; font-size: 8px; line-height: 1.5; }
.icon-command.danger { color: #9b4a3f; }
.recommendation-section h3 { margin: 14px 0 8px; font-size: 13px; }.recommendation-item { padding: 9px 0; border-top: 1px solid #e4e8e9; }.recommendation-item > div:first-child { display: flex; justify-content: space-between; gap: 8px; }.recommendation-item strong { font-size: 12px; }.recommendation-item span,.recommendation-item small { color: #6c797f; font-size: 10px; }.recommendation-item p { margin: 5px 0; color: #52636b; font-size: 10px; line-height: 1.45; }.recommendation-actions { display: flex; gap: 5px; margin-top: 7px; }.recommendation-actions button { min-height: 29px; border: 1px solid #d6dfe0; border-radius: 5px; background: #fff; color: #506169; padding: 0 7px; font-size: 9px; cursor: pointer; }.recommendation-actions button:first-child,.recommendation-actions button.active { border-color: #176d5d; color: #176d5d; }.recommendation-actions .icon-command { margin-left: auto; width: 29px; padding: 0; }
.history-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; position: sticky; bottom: 0; padding: 8px 0; background: #fff; }.history-actions button { min-height: 34px; border: 1px solid #d6dfe0; border-radius: 5px; background: #fff; color: #52636b; font-size: 10px; font-weight: 700; cursor: pointer; }.history-actions i { margin-right: 5px; }.trip-message { padding: 7px; border-radius: 5px; background: #e9f4f1; color: #176d5d; font-size: 11px; }.trip-message.error { background: #faece9; color: #9d3f34; }
.modal-backdrop { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 16px; background: rgba(20,31,36,.52); }.trip-modal { width: min(520px,100%); max-height: 86vh; overflow: auto; padding: 18px; border-radius: 7px; background: #fff; box-shadow: 0 18px 50px rgba(0,0,0,.22); }.trip-modal header { display: flex; justify-content: space-between; align-items: center; }.trip-modal h3 { margin: 0; font-size: 17px; }.trip-modal pre { white-space: pre-wrap; font: 12px/1.65 ui-monospace,Consolas,monospace; }.trip-modal label { display: grid; gap: 6px; margin: 12px 0; color: #52636b; font-size: 11px; font-weight: 700; }.trip-modal select,.trip-modal textarea { width: 100%; border: 1px solid #d6dfe0; border-radius: 6px; padding: 8px; font: inherit; }.trip-modal textarea { min-height: 120px; resize: vertical; }
.spinner { display: inline-block; width: 14px; height: 14px; margin-right: 6px; border: 2px solid rgba(255,255,255,.45); border-top-color: #fff; border-radius: 50%; animation: spin .7s linear infinite; vertical-align: -2px; }@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 760px) { .reroute-strategies { grid-template-columns: 1fr; }.reroute-strategies p { min-height: 0; } }
</style>
