/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */

import {LitElement, html, css} from 'lit';
import { boostrapStyle } from './Bootstrap';

/**
 * An example element.
 *
 * @fires count-changed - Indicates when the count changes
 * @slot - This element has a slot
 * @csspart button - The button
 */
export class Main extends LitElement {
  static get styles() {
    return boostrapStyle;
  }

  render() {
    return html`
      <h1>Hier könnte Ihre Werbung stehen</h1>
      <input class="form-control" type="file" id="formFile">
      <button type="button" class="btn btn-primary">TEST</button>
    `;
  }

  _onClick() {
    this.count++;
    this.dispatchEvent(new CustomEvent('count-changed'));
  }
}

window.customElements.define('service-main', Main);
