/** 移动作业（手机 / PDA，mobile.xxx） */
export default {
  home: {
    backToAdmin: 'Desktop',
    empty: 'No mobile tasks available. Ask your administrator for access'
  },
  scan: {
    placeholder: 'Scan or enter barcode'
  },
  common: {
    currentLocation: 'Location',
    currentLocationN: 'Location {code}',
    selectThenScanSerial: 'Select, then scan serials:',
    scanSerial: '{name}: scan serial No.',
    unknownCode: 'Unknown barcode {code}',
    serialScanned: 'Serial {code} already scanned',
    serialAdded: '{name} serial {code}, total {n}',
    inputBatch: '{name}: enter batch No.',
    scanLocThenItem: 'Scan location, then item / serial',
    batchN: ' Batch {no}',
    notInLocation: '{code} not at this location'
  },
  receive: {
    scanOrder: 'Scan or enter PO No.',
    noOrders: 'No purchase orders to receive',
    changeOrder: 'Change',
    warehouse: 'Receiving warehouse',
    scanTip: 'Scan item or location',
    locationPh: 'Location (auto if empty)',
    thisTime: 'Receiving',
    submit: 'Receive',
    noOpenItems: 'No open items on this order',
    notInOrder: '{code} is not on this order',
    overQty: '{name} exceeds open qty',
    added: '{name} +{qty}, total {total}',
    selectWarehouse: 'Select a receiving warehouse',
    done: 'Received',
    draftSaved: 'Saved as a purchase receipt draft; stock is received after posting'
  },
  putaway: {
    source: 'From',
    change: 'Change',
    scanItem: 'Scan item, then target location',
    scanSource: 'Scan source location (e.g. receiving)',
    qty: 'Qty',
    target: 'Target location',
    noStock: 'No stock to put away here',
    suggest: 'Suggest',
    submit: 'Put Away ({n})',
    sourceN: 'From {code}, {n} items',
    scanItemFirst: 'Scan an item first',
    sameLocation: 'Target must differ from source',
    scanSourceFirst: 'Scan the source location first',
    selected: '{name} selected, scan target location',
    serialNotHere: 'Serial {code} not at this location',
    suggested: '{n} suggested',
    noSuggestion: 'No location to suggest',
    fillMoves: 'Enter qty and target location',
    done: 'Put away',
    pending: 'Transfer order created, awaiting confirmation'
  },
  check: {
    tip: 'Generate a stock count (draft) on desktop, then scan here to enter counted quantities',
    noChecks: 'No stock counts pending',
    change: 'Change count',
    counted: 'Counted {n} / {total}',
    onlyCurrent: 'This location only',
    book: 'Book {n}',
    gain: 'Gain',
    actual: 'Counted',
    actualScan: 'Counted {n}, scan serials:',
    diff: 'Diff lines',
    save: 'Save',
    locationLines: 'Location {code}, {n} lines',
    added: '{name} +{qty}, counted {actual}',
    addedGain: '{name} (gain) +{qty}, counted {actual}',
    serialAdded: '{name} serial {code}, counted {n}',
    inputGainBatch: '{name}: enter batch No. for gain',
    saved: 'Counts saved'
  }
}
