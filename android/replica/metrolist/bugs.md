# Findings and resolution

- M-B01 (S3, fixed): search result taps always opened video previews even when the audio was already saved. Local-first action now opens the native player, with explicit preview available in song options.
- M-B02 (S3, fixed): selecting an item from the local queue rebuilt the library queue and discarded manual order. It now selects within the existing queue.
- M-B03 (S3, fixed): each streamed Next created a fresh WebView. A retained IFrame now loads the next ID; stale video status cannot complete the next item.
- M-B04 (S3, fixed): query submission was blocked until the previous request completed. Superseded subprocesses are now canceled and late callbacks ignored.
- M-B05 (S3, fixed during testing): guest Home placed a tall discovery block before immediate access to saved music. Guest Home now starts with saved rows; Search discovery uses a horizontal shelf.
- M-B06 (S3, fixed during screenshot review): the first Home render did not populate the local-copy index, so a saved account pick lacked its offline marker until visiting Search. Home now indexes local copies before building its rows; the capture test asserts the visible saved marker.
- M-T01 (test setup, resolved): the existing emulator filled its installation partition. A separate AVD in the workspace was created; the original AVD's application data was preserved.
- M-T02 (test setup, resolved): UI automation encountered the first-run notification permission prompt. Instrumentation now grants its declared notification permission explicitly before navigation.
- M-T03 (test timing, resolved): an old media seek check assumed completion in 250 ms. The test now waits for the observable seek result within its normal timeout.

To check: real signed-in feed output; phone-specific playback/focus/Bluetooth behavior; relevance of the general YouTube feed to a music app. These are not reported as reproduced failures or claimed fixed by the emulator tests.
