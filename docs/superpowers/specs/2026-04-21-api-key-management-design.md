# Developer Center Design Spec

## 1. Overview
This project adds a new "Developer Center" (开发者中心) page to the Smart Traffic Monitoring System frontend. The page provides a dedicated UI for API Key management (CRUD) and API Documentation, leveraging the existing backend endpoints (`/api/v1/admin/api-clients` and `/api/v1/api-docs`).

## 2. Navigation & Routing
- **Route:** Add `/developer` route mapping to `DeveloperCenterView.vue` in `frontend/src/router.ts`.
- **Navigation:** Add "开发者中心" (Developer Center) to the main navigation menu in `frontend/src/AppLayout.vue`. This menu item should be visible primarily to administrators, as the underlying API Key CRUD endpoints require admin privileges.

## 3. Page Structure
The `DeveloperCenterView.vue` component will use a `Tabs` layout to split the page into two main sections:
1. **API Keys (密钥管理)**
2. **API Docs (接口文档)**

## 4. API Keys Module
- **List View:** A table displaying the existing API Clients. Columns include: Name, Description, Allowed Endpoints, Rate Limit, and Last Used At.
- **Actions:**
  - **Create Key:** A dialog form to create a new API Client. Fields: Name, Description, Allowed Endpoints (comma-separated), Rate Limit (default 1000). Upon successful creation, the generated `api_key` must be displayed in an alert or dialog with a "Copy to Clipboard" button, as it will only be shown once.
  - **Regenerate Key:** A button to regenerate an existing API Key. This will prompt a confirmation dialog, and upon success, display the new `api_key` with a copy button.
  - **Edit:** A dialog form to update the description, allowed endpoints, and rate limit of an existing API Client.
  - **Delete:** A button to delete an API Client, with a confirmation prompt.
- **API Integration:** Will use the `endpoints.adminApiClients` URL configured in `frontend/src/lib/api.ts`.

## 5. API Docs Module
- **Data Fetching:** The page will fetch data from `/api/v1/api-docs` upon mounting.
- **Header Section:** Display the overall API Title, Version, Base URL, and Authentication Instructions (API Key vs. Bearer Token).
- **Endpoints List:** 
  - Render a list of API endpoints.
  - Each endpoint will have a method badge (e.g., green for GET, blue for POST, yellow for PUT, red for DELETE).
  - Include the path, description, authentication requirement.
  - Render an expandable details section (e.g., using Accordion or a custom toggle) containing:
    - **Parameters:** A table of request parameters (name, type, required, description).
    - **Response Example:** A JSON code block showing the expected response.
    - **cURL Example:** A code block showing how to call the API via `curl`.

## 6. Implementation Notes
- **UI Components:** Use existing shadcn-vue components (`Card`, `Table`, `Button`, `Dialog`, `Input`, `Label`, `Badge`, `Tabs`, `Alert`). If `Accordion` or `Collapsible` components are needed but not present, simple Vue `v-if` toggles can be used for the expandable endpoint details.
- **Security:** Since the API keys are sensitive, ensure the UI clearly warns the user to copy the key immediately upon creation/regeneration.
- **Error Handling:** Use `toast` notifications (from `vue-sonner`) to display success or error messages for all API actions.

## 7. Testing Plan
- Create a new API client, verify the UI displays the key and it can be copied.
- View the API Docs tab, verify the data is fetched and formatted correctly.
- Test the key regeneration and deletion flows.
- Use Playwright via MCP to capture a screenshot of the new UI for visual validation.