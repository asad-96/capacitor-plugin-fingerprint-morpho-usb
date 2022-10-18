# capacitor-plugin-fingerprint-morpho-usb

Fingerprint Morpho Usb

## Install

```bash
npm install capacitor-plugin-fingerprint-morpho-usb
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`openDevice()`](#opendevice)
* [`captureFingerprint(...)`](#capturefingerprint)
* [`compareFingerprint(...)`](#comparefingerprint)
* [`addListener('MorphoEvent', ...)`](#addlistenermorphoevent)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### openDevice()

```typescript
openDevice() => Promise<OpenDeviceResponse>
```

**Returns:** <code>Promise&lt;<a href="#opendeviceresponse">OpenDeviceResponse</a>&gt;</code>

--------------------


### captureFingerprint(...)

```typescript
captureFingerprint(userInfo: UserInfo) => Promise<FingerprintResponse>
```

| Param          | Type                                          |
| -------------- | --------------------------------------------- |
| **`userInfo`** | <code><a href="#userinfo">UserInfo</a></code> |

**Returns:** <code>Promise&lt;<a href="#fingerprintresponse">FingerprintResponse</a>&gt;</code>

--------------------


### compareFingerprint(...)

```typescript
compareFingerprint(userdata: UserData) => Promise<FingerprintResponse>
```

| Param          | Type                                          |
| -------------- | --------------------------------------------- |
| **`userdata`** | <code><a href="#userdata">UserData</a></code> |

**Returns:** <code>Promise&lt;<a href="#fingerprintresponse">FingerprintResponse</a>&gt;</code>

--------------------


### addListener('MorphoEvent', ...)

```typescript
addListener(eventName: 'MorphoEvent', listenerFunc: (data: { info: any; }) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                           |
| ------------------ | ---------------------------------------------- |
| **`eventName`**    | <code>'MorphoEvent'</code>                     |
| **`listenerFunc`** | <code>(data: { info: any; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### Interfaces


#### OpenDeviceResponse

| Prop          | Type                 |
| ------------- | -------------------- |
| **`success`** | <code>boolean</code> |
| **`error`**   | <code>string</code>  |


#### FingerprintResponse

| Prop            | Type                 |
| --------------- | -------------------- |
| **`success`**   | <code>boolean</code> |
| **`message`**   | <code>string</code>  |
| **`rawBitmap`** | <code>any</code>     |
| **`data`**      | <code>any</code>     |


#### UserInfo

| Prop            | Type                |
| --------------- | ------------------- |
| **`userId`**    | <code>string</code> |
| **`firstName`** | <code>string</code> |
| **`lastName`**  | <code>string</code> |


#### UserData

| Prop         | Type                |
| ------------ | ------------------- |
| **`userId`** | <code>string</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

</docgen-api>
