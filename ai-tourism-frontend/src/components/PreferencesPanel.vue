<template>
  <section class="preferences-shell">
    <div class="session-note">
      <i class="fas fa-shield-halved"></i>
      <span>{{ t('sessionOnly') }}</span>
      <strong v-if="saved">{{ t('saved') }}</strong>
    </div>

    <div v-if="loading" class="preference-status"><span class="spinner"></span> {{ t('loadingPreferences') }}</div>
    <form v-else class="preferences-form" @submit.prevent="submit">
      <fieldset>
        <legend>{{ t('interests') }} <em>{{ t('required') }}</em></legend>
        <div class="choice-grid">
          <label v-for="option in interestOptions" :key="option.value" class="choice-chip">
            <input v-model="form.interests" type="checkbox" :value="option.value" />
            <span><i :class="option.icon"></i>{{ option.label }}</span>
          </label>
        </div>
        <p v-if="errors.interests" class="field-error">{{ errors.interests }}</p>
      </fieldset>

      <fieldset>
        <legend>{{ t('currentLocation') }}</legend>
        <div class="location-control">
          <div class="location-summary">
            <i class="fas fa-location-crosshairs"></i>
            <span>
              <strong>{{ form.current_location_name || t('locationNotSet') }}</strong>
              <small v-if="hasLocation">{{ coordinateLabel }}</small>
              <small v-else>{{ t('locationPrivacy') }}</small>
            </span>
            <button v-if="hasLocation" type="button" class="icon-button" :title="t('clearLocation')" @click="clearLocation">
              <i class="fas fa-xmark"></i>
            </button>
          </div>
          <button type="button" class="location-button" :disabled="locating" @click="locateCurrent">
            <span v-if="locating" class="spinner"></span>
            <i v-else class="fas fa-crosshairs"></i>{{ locating ? t('locating') : t('useCurrentLocation') }}
          </button>
          <label class="manual-location-label">
            {{ t('manualLocation') }}
            <select v-model="manualLocationPoiId" @change="useManualLocation">
              <option value="">{{ t('chooseStartPoi') }}</option>
              <option v-for="poi in pois" :key="poi.id" :value="String(poi.id)">{{ poi.name }}</option>
            </select>
          </label>
        </div>
        <p v-if="locationMessage" class="field-error" :class="{ success: !locationError }">{{ locationMessage }}</p>
      </fieldset>

      <fieldset>
        <legend>{{ t('travelTime') }} <em>{{ t('required') }}</em></legend>
        <div class="time-grid">
          <label>{{ t('departure') }}<input v-model="form.departure_time" type="time" required /></label>
          <label>{{ t('latestEnd') }}<input v-model="form.latest_end_time" type="time" required /></label>
        </div>
        <p v-if="errors.time" class="field-error">{{ errors.time }}</p>
      </fieldset>

      <fieldset>
        <legend>{{ t('maxWalking') }}</legend>
        <div class="range-heading"><span>500 {{ t('meter') }}</span><output>{{ walkingLabel }}</output><span>20 {{ t('kilometer') }}</span></div>
        <input v-model.number="form.max_walking_meters" class="walking-range" type="range"
               min="500" max="20000" step="500" />
      </fieldset>

      <fieldset>
        <legend>{{ t('mustVisit') }}</legend>
        <select v-model="poiToAdd" @change="addPoi">
          <option value="">{{ t('choosePoi') }}</option>
          <option v-for="poi in availablePois" :key="poi.id" :value="String(poi.id)">{{ poi.name }}</option>
        </select>
        <div class="selected-pois" v-if="selectedPois.length">
          <span v-for="poi in selectedPois" :key="poi.id">
            {{ poi.name }}
            <button type="button" :title="t('removePoi', { name: poi.name })" @click="removePoi(poi.id)"><i class="fas fa-xmark"></i></button>
          </span>
        </div>
        <p v-if="errors.mustVisit" class="field-error">{{ errors.mustVisit }}</p>
      </fieldset>

      <fieldset>
        <legend>{{ t('transportPreference') }} <em>{{ t('required') }}</em></legend>
        <div class="segmented three">
          <label v-for="option in transportOptions" :key="option.value">
            <input v-model="form.transport_preference" type="radio" :value="option.value" />
            <span><i :class="option.icon"></i>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <fieldset>
        <legend>{{ t('language') }}</legend>
        <div class="segmented four">
          <label v-for="option in languageOptions" :key="option.value">
            <input v-model="form.language" type="radio" :value="option.value" />
            <span>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <fieldset>
        <legend>{{ t('accessibilityNeeds') }}</legend>
        <div class="accessibility-list">
          <label v-for="option in accessibilityOptions" :key="option.value">
            <input v-model="form.accessibility_needs" type="checkbox" :value="option.value" />
            <span>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <p v-if="message" class="form-message" :class="{ error: messageError }">{{ message }}</p>
      <div class="form-actions">
        <button type="button" class="secondary" :disabled="saving" @click="reset">
          <i class="fas fa-arrow-rotate-left"></i> {{ t('reset') }}
        </button>
        <button class="primary" :disabled="saving">
          <span v-if="saving" class="spinner light"></span>
          <i v-else class="fas fa-check"></i>{{ saving ? t('saving') : t('savePreferences') }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { fetchPreferences, resetPreferences, savePreferences } from '../utils/api.js'
import { translate } from '../utils/i18n.js'

const props = defineProps({
  pois: { type: Array, default: () => [] },
  language: { type: String, default: 'zh-Hans' }
})
const emit = defineEmits(['language-change', 'location-change'])

const loading = ref(true)
const saving = ref(false)
const locating = ref(false)
const saved = ref(false)
const updatedAt = ref(null)
const poiToAdd = ref('')
const manualLocationPoiId = ref('')
const message = ref('')
const messageError = ref(false)
const locationMessage = ref('')
const locationError = ref(false)
const errors = reactive({ interests: '', time: '', mustVisit: '' })
const form = reactive({
  interests: [], departure_time: '09:00', latest_end_time: '14:00',
  max_walking_meters: 5000, must_visit_poi_ids: [], transport_preference: 'MIXED',
  language: 'zh-Hans', accessibility_needs: [], current_longitude: null,
  current_latitude: null, current_location_name: '', location_source: null
})

const t = (key, params) => translate(props.language, key, params)
const interestOptions = computed(() => [
  { value: 'ATTRACTION', label: t('interest_ATTRACTION'), icon: 'fas fa-landmark' },
  { value: 'CULTURE', label: t('interest_CULTURE'), icon: 'fas fa-masks-theater' },
  { value: 'FOOD', label: t('interest_FOOD'), icon: 'fas fa-utensils' },
  { value: 'NATURE', label: t('interest_NATURE'), icon: 'fas fa-leaf' },
  { value: 'RETAIL', label: t('interest_RETAIL'), icon: 'fas fa-store' },
  { value: 'PUBLIC_SERVICE', label: t('interest_PUBLIC_SERVICE'), icon: 'fas fa-circle-info' }
])
const transportOptions = computed(() => [
  { value: 'WALK', label: t('transport_WALK'), icon: 'fas fa-person-walking' },
  { value: 'PUBLIC_TRANSIT', label: t('transport_PUBLIC_TRANSIT'), icon: 'fas fa-bus' },
  { value: 'MIXED', label: t('transport_MIXED'), icon: 'fas fa-shuffle' }
])
const languageOptions = [
  { value: 'zh-Hans', label: '简' }, { value: 'zh-Hant', label: '繁' },
  { value: 'en', label: 'EN' }, { value: 'pt', label: 'PT' }
]
const accessibilityOptions = computed(() => [
  { value: 'STEP_FREE', label: t('need_STEP_FREE') },
  { value: 'LOW_WALKING', label: t('need_LOW_WALKING') },
  { value: 'QUIET_ROUTE', label: t('need_QUIET_ROUTE') }
])

const walkingLabel = computed(() => form.max_walking_meters >= 1000
  ? `${(form.max_walking_meters / 1000).toFixed(1)} ${t('kilometer')}`
  : `${form.max_walking_meters} ${t('meter')}`)
const selectedPois = computed(() => form.must_visit_poi_ids
  .map(id => props.pois.find(poi => Number(poi.id) === Number(id)))
  .filter(Boolean))
const availablePois = computed(() => props.pois.filter(poi =>
  !form.must_visit_poi_ids.some(id => Number(id) === Number(poi.id))))
const hasLocation = computed(() => form.current_longitude !== null && form.current_latitude !== null &&
  Number.isFinite(Number(form.current_longitude)) && Number.isFinite(Number(form.current_latitude)))
const coordinateLabel = computed(() => hasLocation.value
  ? `${Number(form.current_latitude).toFixed(5)}, ${Number(form.current_longitude).toFixed(5)}` : '')

function applyPreferences(data) {
  form.interests = [...(data.interests || [])]
  form.departure_time = data.departure_time
  form.latest_end_time = data.latest_end_time
  form.max_walking_meters = data.max_walking_meters
  form.must_visit_poi_ids = [...(data.must_visit_poi_ids || [])]
  form.transport_preference = data.transport_preference
  form.language = data.language
  form.accessibility_needs = [...(data.accessibility_needs || [])]
  form.current_longitude = data.current_longitude ?? null
  form.current_latitude = data.current_latitude ?? null
  form.current_location_name = data.current_location_name || ''
  form.location_source = data.location_source || null
  saved.value = !!data.saved
  updatedAt.value = data.updated_at
  emitLocation()
}

function emitLocation(focus = false) {
  const longitude = Number(form.current_longitude)
  const latitude = Number(form.current_latitude)
  if (form.current_longitude === null || form.current_latitude === null || !Number.isFinite(longitude) || !Number.isFinite(latitude)) {
    emit('location-change', null)
    return
  }
  emit('location-change', {
    longitude,
    latitude,
    name: form.current_location_name || t('currentPosition'),
    source: form.location_source || 'GPS',
    focus
  })
}

function validate() {
  errors.interests = form.interests.length ? '' : t('validationInterest')
  errors.time = form.departure_time < form.latest_end_time ? '' : t('validationTime')
  errors.mustVisit = form.must_visit_poi_ids.length <= 8 ? '' : t('validationMustVisit')
  return !errors.interests && !errors.time && !errors.mustVisit
}

function addPoi() {
  if (!poiToAdd.value) return
  const id = Number(poiToAdd.value)
  if (!form.must_visit_poi_ids.includes(id) && form.must_visit_poi_ids.length < 8) {
    form.must_visit_poi_ids.push(id)
  }
  poiToAdd.value = ''
}

function removePoi(id) {
  form.must_visit_poi_ids = form.must_visit_poi_ids.filter(item => Number(item) !== Number(id))
}

function setLocation(longitude, latitude, name, source) {
  form.current_longitude = Number(Number(longitude).toFixed(6))
  form.current_latitude = Number(Number(latitude).toFixed(6))
  form.current_location_name = name
  form.location_source = source
  emitLocation(true)
}

function clearLocation() {
  form.current_longitude = null
  form.current_latitude = null
  form.current_location_name = ''
  form.location_source = null
  emitLocation()
  manualLocationPoiId.value = ''
  locationError.value = false
  locationMessage.value = t('locationCleared')
}

function useManualLocation() {
  const poi = props.pois.find(item => Number(item.id) === Number(manualLocationPoiId.value))
  if (!poi) return
  setLocation(poi.longitude, poi.latitude, poi.name, 'MANUAL')
  locationError.value = false
  locationMessage.value = t('manualLocationSet', { name: poi.name })
}

function reverseGeocode(longitude, latitude) {
  return new Promise(resolve => {
    if (!window.AMap?.plugin) return resolve(t('currentPosition'))
    window.AMap.plugin('AMap.Geocoder', () => {
      const geocoder = new window.AMap.Geocoder({ city: '澳门', radius: 500 })
      geocoder.getAddress([longitude, latitude], (status, result) => {
        const address = status === 'complete' ? result?.regeocode?.formattedAddress : ''
        resolve(address || t('currentPosition'))
      })
    })
  })
}

function locateCurrent() {
  locationMessage.value = ''
  if (!navigator.geolocation) {
    locationError.value = true
    locationMessage.value = t('geolocationUnsupported')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(async position => {
    const { longitude, latitude } = position.coords
    if (longitude < 113.4 || longitude > 113.7 || latitude < 22.0 || latitude > 22.3) {
      locationError.value = true
      locationMessage.value = t('outsideMacao')
      locating.value = false
      return
    }
    const name = await reverseGeocode(longitude, latitude)
    setLocation(longitude, latitude, name, 'GPS')
    manualLocationPoiId.value = ''
    locationError.value = false
    locationMessage.value = t('locationReady')
    locating.value = false
  }, error => {
    locationError.value = true
    locationMessage.value = error.code === 1 ? t('locationDenied') : t('locationFailed')
    locating.value = false
  }, { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 })
}

async function submit() {
  message.value = ''
  if (!validate()) return
  saving.value = true
  try {
    const data = await savePreferences({ ...form })
    applyPreferences(data)
    messageError.value = false
    message.value = t('saveSuccess')
    emit('language-change', form.language)
  } catch (error) {
    messageError.value = true
    message.value = t('saveFailed')
  } finally {
    saving.value = false
  }
}

async function reset() {
  saving.value = true
  message.value = ''
  try {
    const data = await resetPreferences()
    applyPreferences(data)
    Object.assign(errors, { interests: '', time: '', mustVisit: '' })
    manualLocationPoiId.value = ''
    locationMessage.value = ''
    messageError.value = false
    message.value = t('resetSuccess')
    emit('language-change', form.language)
  } catch (error) {
    messageError.value = true
    message.value = t('resetFailed')
  } finally {
    saving.value = false
  }
}

watch(() => form.language, language => emit('language-change', language))
watch(() => props.language, language => {
  if (language && form.language !== language) form.language = language
})

onMounted(async () => {
  try {
    applyPreferences(await fetchPreferences())
    emit('language-change', form.language)
  } catch (error) {
    messageError.value = true
    message.value = t('preferenceLoadFailed')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.preferences-shell { flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; }
.session-note { min-height: 36px; display: flex; align-items: center; gap: 7px; padding: 8px 10px; margin-bottom: 12px; border: 1px solid #cfe0dc; border-radius: 6px; background: #edf6f3; color: #335e55; font-size: 11px; }
.session-note strong { margin-left: auto; color: #176d5d; }
.preference-status { padding: 30px 8px; text-align: center; color: #66757f; font-size: 13px; }
.preferences-form { display: flex; flex-direction: column; gap: 14px; padding-bottom: 4px; }
fieldset { min-width: 0; margin: 0; padding: 0; border: 0; }
legend { width: 100%; margin-bottom: 8px; color: #4f6069; font-size: 11px; font-weight: 800; }
legend em { color: #a64b3d; font-style: normal; font-weight: 600; }
.choice-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.choice-chip input, .segmented input { position: absolute; opacity: 0; pointer-events: none; }
.choice-chip span { min-height: 34px; display: flex; align-items: center; justify-content: center; gap: 5px; padding: 0 5px; border: 1px solid #dce3e4; border-radius: 5px; color: #52636b; font-size: 11px; cursor: pointer; }
.choice-chip input:checked + span { border-color: #176d5d; background: #e5f1ee; color: #176d5d; font-weight: 800; }
.time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.time-grid label { color: #66757f; font-size: 10px; }
.time-grid input, fieldset > select { width: 100%; height: 36px; margin-top: 5px; padding: 0 8px; border: 1px solid #dce3e4; border-radius: 6px; background: #fff; color: #17242d; }
.range-heading { display: flex; align-items: center; justify-content: space-between; color: #7c898f; font-size: 9px; }
.range-heading output { color: #176d5d; font-size: 12px; font-weight: 800; }
.walking-range { width: 100%; accent-color: #176d5d; }
.location-control { display: grid; grid-template-columns: minmax(118px, .85fr) minmax(0, 1.15fr); gap: 8px; align-items: end; }
.location-summary { grid-column: 1 / -1; min-height: 42px; display: flex; align-items: center; gap: 9px; padding: 7px 9px; border-left: 3px solid #248d77; background: #f1f7f5; }
.location-summary > i { color: #248d77; font-size: 15px; }
.location-summary span { min-width: 0; display: flex; flex: 1; flex-direction: column; gap: 2px; }
.location-summary strong { overflow: hidden; color: #263940; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.location-summary small { color: #6b7b80; font-size: 9px; }
.icon-button { width: 28px; height: 28px; border: 0; background: transparent; color: #65767c; cursor: pointer; }
.location-button { width: 100%; min-width: 0; min-height: 40px; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 6px 8px; border: 1px solid #248d77; border-radius: 6px; background: #fff; color: #176d5d; font-size: 11px; font-weight: 800; line-height: 1.25; overflow-wrap: anywhere; text-align: center; white-space: normal; cursor: pointer; }
.location-button > i, .location-button > .spinner { flex: 0 0 auto; }
.location-button > .spinner { margin-right: 0; }
.location-button:disabled { opacity: .6; cursor: wait; }
.manual-location-label { min-width: 0; color: #66757f; font-size: 9px; }
.manual-location-label select { width: 100%; height: 36px; margin-top: 4px; padding: 0 7px; border: 1px solid #dce3e4; border-radius: 6px; background: #fff; color: #17242d; }
.field-error.success { color: #176d5d; }
.selected-pois { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 7px; }
.selected-pois > span { display: inline-flex; align-items: center; gap: 5px; max-width: 100%; padding: 5px 6px 5px 8px; border-radius: 4px; background: #edf3f1; color: #36594f; font-size: 10px; }
.selected-pois button { width: 18px; height: 18px; border: 0; background: transparent; color: #60726c; cursor: pointer; }
.segmented { display: grid; gap: 3px; padding: 3px; border: 1px solid #dce3e4; border-radius: 6px; }
.segmented.three { grid-template-columns: repeat(3, 1fr); }
.segmented.four { grid-template-columns: repeat(4, 1fr); }
.segmented span { height: 30px; display: flex; align-items: center; justify-content: center; gap: 5px; border-radius: 4px; color: #66757f; font-size: 11px; cursor: pointer; }
.segmented input:checked + span { background: #e4f0ed; color: #176d5d; font-weight: 800; }
.accessibility-list { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.accessibility-list label { display: flex; align-items: center; gap: 6px; color: #52636b; font-size: 11px; }
.accessibility-list input { width: 15px; height: 15px; accent-color: #176d5d; }
.field-error, .form-message { margin: 6px 0 0; color: #a13c2f; font-size: 11px; }
.form-message { margin: 0; padding: 7px 9px; border-radius: 5px; background: #e9f4f1; color: #176d5d; }
.form-message.error { background: #faece9; color: #a13c2f; }
.form-actions { display: grid; grid-template-columns: 100px 1fr; gap: 8px; position: sticky; bottom: 0; padding-top: 5px; background: #fff; }
.form-actions button { height: 38px; border-radius: 6px; cursor: pointer; font-weight: 800; }
.form-actions .secondary { border: 1px solid #dce3e4; background: #fff; color: #53646d; }
.form-actions .primary { border: 1px solid #176d5d; background: #176d5d; color: #fff; }
.form-actions button:disabled { opacity: .65; cursor: wait; }
.spinner { display: inline-block; width: 14px; height: 14px; margin-right: 6px; border: 2px solid #b8cfca; border-top-color: #176d5d; border-radius: 50%; animation: spin .7s linear infinite; vertical-align: -2px; }
.spinner.light { border-color: rgba(255,255,255,.45); border-top-color: #fff; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 760px) {
  .preferences-form { gap: 11px; }
  .session-note { margin-bottom: 9px; }
  .choice-grid { grid-template-columns: repeat(3, 1fr); }
  .location-control { grid-template-columns: 1fr; }
  .location-summary { grid-column: 1; }
}
</style>
