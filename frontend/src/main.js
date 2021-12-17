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
  imageBase64 = '';
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
    let reader = this._getBase64(file, filePreviewer);

    console.log(reader.result);
  };

  _getBase64 = function (file, imageComponent) {
    let reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = function () {
      this.imageBase64 = reader.result;

      // preview.
      imageComponent.src = this.imageBase64;
    };
    return reader;
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

    let request = {
      image: this.image.name,
      extension: this.image.type,
      models: this.models,
      sessionkey: 'sessionkey',
    };

    console.log(request);

    messageComponent.innerHTML = `
        <span style="color: blue; font-weight: normal;">
          Uploading ...
        </span>`;

    // console.log(this.imageBase64);

    let outputImageComponent = this.renderRoot.querySelector('#output-preview');
    // outputImageComponent.src = this.image;
    this._getBase64(this.image, outputImageComponent);

    const wsUri = this.wsUri + this.wsPort + "/websocket";

    console.log(`Websocket to '${wsUri}'`);

    const socket = new WebSocket(wsUri);

    socket.addEventListener('open', function(event) {
      socket.send('Hello Server! ' + JSON.stringify(request));
    });

    socket.addEventListener('message', function(event) {
      console.log('Received Message: ' + event.data);
    });
  };
}

window.customElements.define('service-main', Main);
