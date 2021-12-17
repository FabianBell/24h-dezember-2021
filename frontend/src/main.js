/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */

import {LitElement, html, css} from 'lit';
import {boostrapStyle} from './Bootstrap';
import {repeat} from 'lit/directives/repeat.js';

/**
 * An example element.
 *
 * @fires count-changed - Indicates when the count changes
 * @slot - This element has a slot
 * @csspart button - The button
 */
export class Main extends LitElement {
  image = null;
  imageByteArray = [];
  models = [];

  wsUri = 'ws://localhost:';
  wsPort = '8080';

  modeloptions = ['GFPGAN', 'ARCANA'];

  static get styles() {
    return boostrapStyle;
  }

  render() {
    return html`
      <div style="padding: 1%;">
        <h1>Hier k&ouml;nnte Ihre Werbung stehen</h1>

        <div style="padding:7px;">
          <div style="padding: 7px 0px;">
            Upload your image:
            <input
              id="inputImage"
              class="form-control"
              type="file"
              value="${this.image || ''}"
              @change="${this._updateFile}"
            />
            <img
              id="inputImage-preview"
              src=""
              style="width:100%; padding: 5%;"
            />
          </div>
          <div style="padding: 7px 0px;">
            Choose your model.
            <div>
              <ol id="models">
                ${repeat(
                  this.models,
                  (chosenModell) => html`<li>${chosenModell}</li>`
                )}
                ${this.models.map(m => html`<li>${m}</li>`)}
              </ol>
              ${repeat(
                this.modeloptions,
                (option) => html`<button @click=${() => this._apply(option) }>${option}</button>`
              )}
              <button @click="${() => console.log('Nope')}">...</button>
            </div>
          </div>
          <div style="padding: 7px 0px;">
            <button
              id="submit"
              type="button"
              class="btn btn-primary"
              @click="${this._submit}"
              style="width: 100%;"
            >
              Submit !
            </button>
          </div>
          <div style="padding: 7px 0px;">
            <img
              id="output-preview"
              src="https://huggingface.co/front/assets/huggingface_logo-noborder.svg"
              style="width:100%; padding: 5%;"
            />
            <p id="message"></p>
          </div>
        </div>
      </div>
    `;
  }

  _apply = function (model) {
    this.models.push(model);
    console.log(this.models);
  };

  _updateFile = function () {
    // get latest file.
    let fileInput = this.renderRoot.querySelector('#inputImage');

    let file = fileInput.files[0];
    console.log(file);

    this.image = file;
    console.log(this.image);

    // preview file.
    let filePreviewer = this.renderRoot.querySelector('#inputImage-preview');
    let reader = this._previewImage(file, filePreviewer);

    console.log(reader.result);
  };

  _previewImage = function (file, imageComponent) {
    this._readAsBase64(file, (base64) => { imageComponent.src = base64 });
  };

  _readAsBase64 = function(file, callback) {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = function(event) {
      let base64 = reader.result;
      callback(base64);
    };
  };

  _submit = function () {
    let messageComponent = this.renderRoot.querySelector('#message');

    // --- SEND REQUEST TO CONTROLLER. ---------------------------------------
    if (!this.image) {
      messageComponent.innerHTML = `
        <span style="color: red; font-weight: bolder;">
          Could not upload this image.
        </span>`;
      return;
    }

    if (!this.models || this.models.length < 1) {
      this.models = [this.modeloptions[0]];
      console.log("adding default model", this.models);
    }

    const extension = this.image.type.split('/').pop();
    const models = this.models;

    const wsUri = this.wsUri + this.wsPort + "/websocket";

    let outputImageComponent = this.renderRoot.querySelector('#output-preview');
    this._previewImage(this.image, outputImageComponent);

    this._readAsBase64(this.image, function(base64) {

      // max 65.536
      console.log("base64", base64.length);
      console.log("base64", base64);

      let chunks64 = [];

      let i = 0;
      const n = base64.length;
      const chunk = 10000;

      for (i; i < n; i += chunk) {
        chunks64.push(base64.slice(i, i +  chunk));
      }

      console.log(chunks64);

      console.log(`Websocket to '${wsUri}'`);

      const socket = new WebSocket(wsUri);

      // EXPECTED:
      // { session: string, img: bytecode, extension: string(png|jpeg|...), models: string[] }
      socket.addEventListener('open', function(event) {

        // create request.
        const request = {
          session: 'sessionkey',
          extension,
          models: models,
          count: chunks64.length,
        };

        socket.send(JSON.stringify(request));
        console.log('Send request', JSON.stringify(request));

        let i = 0;
        let n = chunks64.length;

        for (; i < n; i++) {
          const request0 = {
            session: 'sessionkey',
            index: i,
            img: chunks64[i],
          };
          socket.send(JSON.stringify(request0));
          console.log('Send request', JSON.stringify(request0));
        }
      });

      // EXPECTED:
      // receive image as string.
      socket.addEventListener('message', function(event) {
        console.log('Received Message: ' + event.data);
        outputImageComponent.src = event.data;
      });
    });

    // show state.
    messageComponent.innerHTML = `
        <span style="color: blue; font-weight: normal;">
          Uploading ...
        </span>`;
  };
}

window.customElements.define('service-main', Main);
